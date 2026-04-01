import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import './admin.css';
import 'bootstrap';
import { Modal } from 'bootstrap';

import { initI18n } from '../../src/shared/js/i18n.ts';
import { initNavbar } from '../../src/shared/js/navbar.ts';
import { injectNavbar } from '../../src/shared/js/inject-navbar.ts';
import { injectFooter } from '../../src/shared/js/footer.ts';
import { requireAdmin, fetchAdminStats, formatDate, statusBadge } from './admin-utils.ts';
import { apiGet, apiPost, apiPut, apiPatch, apiDelete } from '../../src/shared/js/api.ts';
import { toast } from '../../src/shared/js/toast.ts';
import type { UserResponse, CourseResponse, AdminEnrollmentResponse, Page, CreateAdminUserRequest } from '../../src/shared/js/types.ts';

// ─── Lazy load flags ────────────────────────────────────────────
let usersLoaded      = false;
let coursesLoaded    = false;
let enrollsLoaded    = false;

// ─── User state ─────────────────────────────────────────────────
let userPage         = 0;
let userTotalPages   = 0;
let userKeyword      = '';
let userRole         = '';
let userSearchTimer: ReturnType<typeof setTimeout> | null = null;
let editingUserId: number | null = null;
let roleModalInst: InstanceType<typeof Modal> | null = null;
let createUserModalInst: InstanceType<typeof Modal> | null = null;

// ─── Course state ────────────────────────────────────────────────
let coursePage         = 0;
let courseTotalPages   = 0;
let courseKeyword      = '';
let coursePublished    = '';
let courseCategory     = '';
let courseModalInst: InstanceType<typeof Modal> | null = null;
let courseIsEdit       = false;

// ─── Enrollment state ────────────────────────────────────────────
let enrollPage       = 0;
let enrollTotalPages = 0;
let enrollStatus     = 'pending';
let revokeModalInst: InstanceType<typeof Modal> | null = null;
let pendingRevokeId: number | null = null;

// ─── Schedule state (removed – now managed in Moodle) ────────

// ════════════════════════════════════════════════════════════════
//   DASHBOARD
// ════════════════════════════════════════════════════════════════

async function loadStats(): Promise<void> {
  try {
    const [stats, activeEnrollPage] = await Promise.all([
      fetchAdminStats(),
      apiGet<Page<unknown>>('/admin/enrollments?status=active&size=1').catch(() => ({ totalElements: '?' as number | string })),
    ]);
    const el = (id: string) => document.getElementById(id);
    if (el('statUsers'))   el('statUsers')!.textContent   = String(stats.totalUsers   ?? 0);
    if (el('statCourses')) el('statCourses')!.textContent = String(stats.totalCourses  ?? 0);
    if (el('statPending')) el('statPending')!.textContent = String(stats.pendingEnrollments ?? 0);
    if (el('statActive'))  el('statActive')!.textContent  = String(activeEnrollPage.totalElements ?? 0);
  } catch (err) {
    console.error('Failed to load admin stats', err);
  }
}

// ════════════════════════════════════════════════════════════════
//   USERS
// ════════════════════════════════════════════════════════════════

function userRoleBadge(role: string): string {
  const map: Record<string, string> = { ADMIN: 'bg-danger', TEACHER: 'bg-success', STUDENT: 'bg-primary' };
  return `<span class="badge ${map[role] || 'bg-secondary'}">${role}</span>`;
}

function renderUserRow(u: UserResponse): string {
  const active = u.active
    ? '<span class="badge bg-success-subtle text-success">Hoạt động</span>'
    : '<span class="badge bg-secondary">Tắt</span>';
  return `<tr>
    <td class="text-muted small">${u.id}</td>
    <td><span class="fw-medium">${u.username}</span></td>
    <td class="d-none d-md-table-cell">${u.firstName || ''} ${u.lastName || ''}</td>
    <td class="d-none d-lg-table-cell small">${u.email}</td>
    <td>${userRoleBadge(u.role)}</td>
    <td>${active}</td>
    <td class="d-none d-lg-table-cell small">${formatDate(u.createdAt)}</td>
    <td>
      <button class="btn btn-xs btn-outline-secondary btn-sm py-0 me-1" data-action="role"
        data-id="${u.id}" data-username="${u.username}" data-role="${u.role}" title="Đổi vai trò">
        <i class="fa fa-user-tag"></i>
      </button>
      <button class="btn btn-xs ${u.active ? 'btn-outline-warning' : 'btn-outline-success'} btn-sm py-0"
        data-action="toggle" data-id="${u.id}" title="${u.active ? 'Vô hiệu hóa' : 'Kích hoạt'}">
        <i class="fa ${u.active ? 'fa-ban' : 'fa-check'}"></i>
      </button>
    </td>
  </tr>`;
}

async function loadUsers(page: number = 0): Promise<void> {
  const tbody = document.getElementById('userTableBody')!;
  tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';

  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (userRole)    params.set('role', userRole);
  if (userKeyword) params.set('keyword', userKeyword);

  try {
    const data = await apiGet<Page<UserResponse>>(`/admin/users?${params.toString()}`);
    userPage       = data.number;
    userTotalPages = data.totalPages;

    tbody.innerHTML = data.content.length === 0
      ? '<tr><td colspan="8" class="text-center text-muted py-4">Không tìm thấy người dùng nào.</td></tr>'
      : data.content.map(renderUserRow).join('');

    tbody.querySelectorAll<HTMLElement>('[data-action]').forEach(btn => btn.addEventListener('click', handleUserAction));

    const pi = document.getElementById('userPageInfo');
    if (pi) pi.textContent = `Trang ${userPage + 1} / ${userTotalPages || 1} (${data.totalElements ?? 0} mục)`;
    (document.getElementById('userPrev') as HTMLButtonElement).disabled = userPage === 0;
    (document.getElementById('userNext') as HTMLButtonElement).disabled = userPage >= userTotalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

function handleUserAction(e: Event): void {
  const btn    = e.currentTarget as HTMLElement;
  const action = btn.dataset.action;
  const id     = Number(btn.dataset.id);
  if (action === 'role') {
    editingUserId = id;
    document.getElementById('roleModalUsername')!.textContent = btn.dataset.username!;
    (document.getElementById('roleSelect') as HTMLSelectElement).value = btn.dataset.role!;
    roleModalInst?.show();
  } else if (action === 'toggle') {
    toggleActive(id, btn as HTMLButtonElement);
  }
}

async function toggleActive(id: number, btn: HTMLButtonElement): Promise<void> {
  btn.disabled = true;
  try {
    await apiPatch(`/admin/users/${id}/active`);
    loadUsers(userPage);
  } catch (err: any) {
    toast.error(err.message);
    btn.disabled = false;
  }
}

async function saveRole(): Promise<void> {
  if (!editingUserId) return;
  const role    = (document.getElementById('roleSelect') as HTMLSelectElement).value;
  const saveBtn = document.getElementById('saveRoleBtn') as HTMLButtonElement;
  saveBtn.disabled = true;
  try {
    await apiPatch(`/admin/users/${editingUserId}/role`, { role });
    roleModalInst?.hide();
    loadUsers(userPage);
  } catch (err: any) {
    toast.error(err.message);
  } finally {
    saveBtn.disabled = false;
  }
}

function clearCreateUserForm(): void {
  (['cuUsername', 'cuPassword', 'cuEmail', 'cuFirstName', 'cuLastName'] as const).forEach(id => {
    (document.getElementById(id) as HTMLInputElement).value = '';
  });
  (document.getElementById('cuRole') as HTMLSelectElement).value = 'STUDENT';
  const alertEl = document.getElementById('cuAlert')!;
  alertEl.className   = 'alert d-none';
  alertEl.textContent = '';
}

async function saveNewUser(): Promise<void> {
  const alertEl = document.getElementById('cuAlert')!;
  const saveBtn = document.getElementById('saveNewUserBtn') as HTMLButtonElement;

  const username  = (document.getElementById('cuUsername') as HTMLInputElement).value.trim();
  const password  = (document.getElementById('cuPassword') as HTMLInputElement).value;
  const email     = (document.getElementById('cuEmail') as HTMLInputElement).value.trim();
  const firstName = (document.getElementById('cuFirstName') as HTMLInputElement).value.trim();
  const lastName  = (document.getElementById('cuLastName') as HTMLInputElement).value.trim();
  const role      = (document.getElementById('cuRole') as HTMLSelectElement).value as CreateAdminUserRequest['role'];

  if (!username || !password || !email) {
    alertEl.className   = 'alert alert-warning';
    alertEl.textContent = 'Vui lòng điền đầy đủ username, password và email.';
    return;
  }

  const payload: CreateAdminUserRequest = {
    username,
    password,
    email,
    firstName: firstName || undefined,
    lastName: lastName || undefined,
    role,
  };
  saveBtn.disabled = true;
  try {
    await apiPost('/admin/users', payload);
    createUserModalInst?.hide();
    toast.success(`Tài khoản "${username}" đã được tạo thành công.`);
    loadUsers(0);
  } catch (err: any) {
    alertEl.className   = 'alert alert-danger';
    alertEl.textContent = err.message;
  } finally {
    saveBtn.disabled = false;
  }
}

// ════════════════════════════════════════════════════════════════
//   COURSES
// ════════════════════════════════════════════════════════════════

function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
function escAttr(s: string | null | undefined): string {
  return String(s ?? '').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function renderCourseRow(c: CourseResponse): string {
  const published = c.published
    ? '<span class="badge bg-success">Công khai</span>'
    : '<span class="badge bg-secondary">Ẩn</span>';
  return `<tr>
    <td class="text-muted small">${c.id}</td>
    <td>
      <div class="fw-medium">${escHtml(c.name)}</div>
      ${c.description ? `<div class="text-muted small text-truncate" style="max-width:260px">${escHtml(c.description)}</div>` : ''}
    </td>
    <td class="d-none d-md-table-cell small">${c.category || '—'}</td>
    <td class="d-none d-md-table-cell small">${c.level || '—'}</td>
    <td class="d-none d-lg-table-cell small">${c.teacherName || '<span class="text-muted">Chưa phân công</span>'}</td>
    <td>${published}</td>
    <td>
      <button class="btn btn-sm btn-outline-primary py-0 me-1" data-action="edit"
        data-id="${c.id}"
        data-name="${escAttr(c.name)}"
        data-category="${escAttr(c.category || '')}"
        data-level="${escAttr(c.level || '')}"
        data-description="${escAttr(c.description || '')}"
        data-imageurl="${escAttr(c.imageUrl || '')}"
        data-teacherid="${c.teacherId || ''}"
        data-published="${c.published}"
        title="Chỉnh sửa"><i class="fa fa-pen"></i></button>
      <button class="btn btn-sm btn-outline-danger py-0" data-action="delete"
        data-id="${c.id}" data-name="${escAttr(c.name)}" title="Xóa"><i class="fa fa-trash"></i></button>
    </td>
  </tr>`;
}

async function loadCourses(page: number = 0): Promise<void> {
  const tbody = document.getElementById('courseTableBody')!;
  tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';

  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (courseKeyword)   params.set('keyword', courseKeyword);
  if (coursePublished) params.set('published', coursePublished);
  if (courseCategory)  params.set('category', courseCategory);

  try {
    const data = await apiGet<Page<CourseResponse>>(`/admin/courses?${params.toString()}`);
    coursePage       = data.number;
    courseTotalPages = data.totalPages;

    tbody.innerHTML = data.content.length === 0
      ? '<tr><td colspan="7" class="text-center text-muted py-4">Không có khóa học.</td></tr>'
      : data.content.map(renderCourseRow).join('');

    tbody.querySelectorAll<HTMLElement>('[data-action]').forEach(btn => btn.addEventListener('click', handleCourseAction));

    const pi = document.getElementById('coursePageInfo');
    if (pi) pi.textContent = `Trang ${coursePage + 1} / ${courseTotalPages || 1} (${data.totalElements ?? 0} mục)`;
    (document.getElementById('coursePrev') as HTMLButtonElement).disabled = coursePage === 0;
    (document.getElementById('courseNext') as HTMLButtonElement).disabled = coursePage >= courseTotalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

function handleCourseAction(e: Event): void {
  const btn = e.currentTarget as HTMLElement;
  if (btn.dataset.action === 'edit')     openEditCourseModal(btn.dataset as Record<string, string>);
  if (btn.dataset.action === 'delete')   deleteCourse(Number(btn.dataset.id), btn.dataset.name!);
}

function clearCourseModal(): void {
  ['courseId','courseName','courseCategory','courseDescription','courseImageUrl','courseTeacherId'].forEach(id => {
    const el = document.getElementById(id) as HTMLInputElement | null;
    if (el) el.value = '';
  });
  (document.getElementById('courseLevel') as HTMLSelectElement).value      = '';
  (document.getElementById('coursePublished') as HTMLInputElement).checked = true;
  document.getElementById('courseModalAlert')!.className = 'alert d-none';
}

function openCreateCourseModal(): void {
  courseIsEdit = false;
  clearCourseModal();
  document.getElementById('courseModalTitle')!.textContent = 'Thêm khóa học';
  courseModalInst?.show();
}

function openEditCourseModal(d: Record<string, string>): void {
  courseIsEdit = true;
  clearCourseModal();
  document.getElementById('courseModalTitle')!.textContent  = 'Chỉnh sửa khóa học';
  (document.getElementById('courseId') as HTMLInputElement).value                = d.id;
  (document.getElementById('courseName') as HTMLInputElement).value              = d.name;
  (document.getElementById('courseCategory') as HTMLInputElement).value          = d.category || '';
  (document.getElementById('courseLevel') as HTMLSelectElement).value             = d.level || '';
  (document.getElementById('courseDescription') as HTMLTextAreaElement).value       = d.description || '';
  (document.getElementById('courseImageUrl') as HTMLInputElement).value          = d.imageurl || '';
  (document.getElementById('courseTeacherId') as HTMLInputElement).value         = d.teacherid || '';
  (document.getElementById('coursePublished') as HTMLInputElement).checked       = d.published === 'true';
  courseModalInst?.show();
}

async function saveCourse(): Promise<void> {
  const saveBtn = document.getElementById('saveCourseBtn') as HTMLButtonElement;
  const alertEl = document.getElementById('courseModalAlert')!;
  const name    = (document.getElementById('courseName') as HTMLInputElement).value.trim();
  if (!name) {
    alertEl.className   = 'alert alert-warning';
    alertEl.textContent = 'Tên khóa học không được để trống.';
    return;
  }
  const teacherRaw = (document.getElementById('courseTeacherId') as HTMLInputElement).value.trim();
  const payload = {
    name,
    category:    (document.getElementById('courseCategory') as HTMLInputElement).value.trim() || null,
    level:       (document.getElementById('courseLevel') as HTMLSelectElement).value || null,
    description: (document.getElementById('courseDescription') as HTMLTextAreaElement).value.trim() || null,
    imageUrl:    (document.getElementById('courseImageUrl') as HTMLInputElement).value.trim() || null,
    // Send 0 to clear teacher (backend: teacherId == 0 → unassign); non-empty value assigns
    teacherId:   teacherRaw !== '' ? Number(teacherRaw) : 0,
    published:   (document.getElementById('coursePublished') as HTMLInputElement).checked,
  };
  saveBtn.disabled = true;
  try {
    if (courseIsEdit) {
      const id = (document.getElementById('courseId') as HTMLInputElement).value;
      await apiPut(`/admin/courses/${id}`, payload);
    } else {
      await apiPost('/admin/courses', payload);
    }
    courseModalInst?.hide();
    loadCourses(coursePage);
  } catch (err: any) {
    alertEl.className   = 'alert alert-danger';
    alertEl.textContent = err.message;
  } finally {
    saveBtn.disabled = false;
  }
}

async function deleteCourse(id: number, name: string): Promise<void> {
  if (!confirm(`Xóa khóa học "${name}"?\n\nLưu ý: không thể xóa nếu có học viên đã đăng ký.`)) return;
  try {
    await apiDelete(`/admin/courses/${id}`);
    loadCourses(coursePage);
  } catch (err: any) {
    toast.error(err.message);
  }
}

// ════════════════════════════════════════════════════════════════
//   ENROLLMENTS
// ════════════════════════════════════════════════════════════════

const enrollStatusTitles: Record<string, string> = {
  pending: 'Danh sách chờ duyệt',
  active:  'Danh sách đang học',
  '':      'Tất cả đăng ký',
};

function renderEnrollRow(e: AdminEnrollmentResponse): string {
  const progress = `<div class="progress" style="height:6px;min-width:60px">
    <div class="progress-bar" style="width:${e.progress ?? 0}%"></div>
  </div><small>${e.progress ?? 0}%</small>`;

  const approveBtn = (e.status === 'pending')
    ? `<button class="btn btn-sm btn-success py-0 me-1" data-action="approve" data-id="${e.id}" title="Duyệt"><i class="fa fa-check"></i></button>`
    : '';
  const revokeBtn = (e.status !== 'revoked')
    ? `<button class="btn btn-sm btn-danger py-0" data-action="revoke" data-id="${e.id}" data-student="${escAttr(e.studentUsername)}" data-course="${escAttr(e.courseName)}" title="Thu hồi"><i class="fa fa-ban"></i></button>`
    : '';

  return `<tr>
    <td class="text-muted small">${e.id}</td>
    <td>
      <div class="fw-medium">${e.studentUsername ?? '—'}</div>
      <div class="text-muted small">${e.studentFullName ?? ''}</div>
    </td>
    <td class="small">${e.courseName ?? '—'}</td>
    <td>${statusBadge(e.status)}</td>
    <td class="d-none d-md-table-cell">${progress}</td>
    <td class="d-none d-lg-table-cell small">${formatDate(e.enrolledAt)}</td>
    <td>${approveBtn}${revokeBtn}</td>
  </tr>`;
}

async function loadEnrollments(page: number = 0): Promise<void> {
  const tbody = document.getElementById('enrollTableBody')!;
  tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';
  try {
    const qs   = enrollStatus ? `status=${enrollStatus}&` : '';
    const data = await apiGet<Page<AdminEnrollmentResponse>>(`/admin/enrollments?${qs}page=${page}&size=20`);
    enrollPage       = data.number;
    enrollTotalPages = data.totalPages;

    tbody.innerHTML = data.content.length === 0
      ? '<tr><td colspan="7" class="text-center text-muted py-4">Không có đăng ký nào.</td></tr>'
      : data.content.map(renderEnrollRow).join('');

    tbody.querySelectorAll<HTMLElement>('[data-action]').forEach(btn => btn.addEventListener('click', handleEnrollAction));

    const pi  = document.getElementById('enrollPageInfo');
    const lbl = document.getElementById('enrollPageLabel');
    const txt = `Trang ${enrollPage + 1} / ${enrollTotalPages || 1} (${data.totalElements ?? 0} mục)`;
    if (pi)  pi.textContent  = txt;
    if (lbl) lbl.textContent = txt;

    (document.getElementById('enrollPrev') as HTMLButtonElement).disabled = enrollPage === 0;
    (document.getElementById('enrollNext') as HTMLButtonElement).disabled = enrollPage >= enrollTotalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

function handleEnrollAction(e: Event): void {
  const btn    = e.currentTarget as HTMLElement;
  const id     = Number(btn.dataset.id);
  if (btn.dataset.action === 'approve') approveEnrollment(id, btn as HTMLButtonElement);
  if (btn.dataset.action === 'revoke')  openRevokeModal(id, btn);
}

async function approveEnrollment(id: number, btn: HTMLButtonElement): Promise<void> {
  btn.disabled = true;
  try {
    await apiPatch(`/admin/enrollments/${id}/approve`);
    loadEnrollments(enrollPage);
  } catch (err: any) {
    toast.error(err.message);
    btn.disabled = false;
  }
}

function openRevokeModal(id: number, btn: HTMLElement): void {
  pendingRevokeId = id;
  document.getElementById('revokeStudent')!.textContent = btn.dataset.student!;
  document.getElementById('revokeCourse')!.textContent  = btn.dataset.course!;
  (document.getElementById('revokeNote') as HTMLTextAreaElement).value = '';
  revokeModalInst?.show();
}

async function confirmRevoke(): Promise<void> {
  if (!pendingRevokeId) return;
  const note    = (document.getElementById('revokeNote') as HTMLTextAreaElement).value.trim();
  const saveBtn = document.getElementById('confirmRevokeBtn') as HTMLButtonElement;
  saveBtn.disabled = true;
  try {
    const qs = note ? `?note=${encodeURIComponent(note)}` : '';
    await apiPatch(`/admin/enrollments/${pendingRevokeId}/revoke${qs}`);
    revokeModalInst?.hide();
    loadEnrollments(enrollPage);
  } catch (err: any) {
    toast.error(err.message);
  } finally {
    saveBtn.disabled = false;
  }
}

// ════════════════════════════════════════════════════════════════
//   COURSE SCHEDULES — Removed (now managed in Moodle LMS)
// ════════════════════════════════════════════════════════════════

// ════════════════════════════════════════════════════════════════
//   INIT
// ════════════════════════════════════════════════════════════════

function switchToTab(tabPaneId: string): void {
  const btn = document.querySelector<HTMLButtonElement>(`[data-bs-target="#${tabPaneId}"]`);
  btn?.click();
}

function initApp(): void {
  requireAdmin();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();

  // ── Modals ────────────────────────────────────────────────────
  roleModalInst        = new Modal(document.getElementById('roleModal')!);
  createUserModalInst  = new Modal(document.getElementById('createUserModal')!);
  courseModalInst      = new Modal(document.getElementById('courseModal')!);
  revokeModalInst      = new Modal(document.getElementById('revokeModal')!);

  // ── Dashboard buttons ─────────────────────────────────────────
  document.querySelectorAll<HTMLButtonElement>('[data-goto-tab]').forEach(btn => {
    btn.addEventListener('click', () => switchToTab(btn.dataset.gotoTab!));
  });

  // ── Tab lazy loading ──────────────────────────────────────────
  document.getElementById('adminTabs')!.addEventListener('show.bs.tab', (e: Event) => {
    const target = (e as any).target as HTMLElement;
    const paneId = target.dataset.bsTarget;
    if (paneId === '#tabUsers' && !usersLoaded) {
      usersLoaded = true;
      loadUsers(0);
    } else if (paneId === '#tabCourses' && !coursesLoaded) {
      coursesLoaded = true;
      loadCourses(0);
    } else if (paneId === '#tabEnrollments' && !enrollsLoaded) {
      enrollsLoaded = true;
      loadEnrollments(0);
    }
  });

  // ── Users ─────────────────────────────────────────────────────
  document.getElementById('saveRoleBtn')!.addEventListener('click', saveRole);
  document.getElementById('createUserBtn')!.addEventListener('click', () => {
    clearCreateUserForm();
    createUserModalInst?.show();
  });
  document.getElementById('saveNewUserBtn')!.addEventListener('click', saveNewUser);

  document.getElementById('userApplyFilter')!.addEventListener('click', () => {
    userKeyword = (document.getElementById('userSearch') as HTMLInputElement).value.trim();
    userRole    = (document.getElementById('userRoleFilter') as HTMLSelectElement).value;
    loadUsers(0);
  });
  document.getElementById('userSearch')!.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      if (userSearchTimer) clearTimeout(userSearchTimer);
      userKeyword = (e.target as HTMLInputElement).value.trim();
      loadUsers(0);
    }
  });
  document.getElementById('userSearch')!.addEventListener('input', (e: Event) => {
    if (userSearchTimer) clearTimeout(userSearchTimer);
    userSearchTimer = setTimeout(() => {
      userKeyword = (e.target as HTMLInputElement).value.trim();
      loadUsers(0);
    }, 500);
  });
  document.getElementById('userPrev')!.addEventListener('click', () => loadUsers(userPage - 1));
  document.getElementById('userNext')!.addEventListener('click', () => loadUsers(userPage + 1));

  // ── Courses ───────────────────────────────────────────────────
  document.getElementById('addCourseBtn')!.addEventListener('click', openCreateCourseModal);
  document.getElementById('saveCourseBtn')!.addEventListener('click', saveCourse);
  document.getElementById('courseApplyFilter')!.addEventListener('click', () => {
    courseKeyword   = (document.getElementById('courseSearch') as HTMLInputElement).value.trim();
    coursePublished = (document.getElementById('coursePublishedFilter') as HTMLSelectElement).value;
    courseCategory  = (document.getElementById('courseCategoryFilter') as HTMLInputElement).value.trim();
    loadCourses(0);
  });
  document.getElementById('courseSearch')!.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      courseKeyword = (e.target as HTMLInputElement).value.trim();
      loadCourses(0);
    }
  });
  document.getElementById('coursePrev')!.addEventListener('click', () => loadCourses(coursePage - 1));
  document.getElementById('courseNext')!.addEventListener('click', () => loadCourses(coursePage + 1));

  // ── Enrollments ───────────────────────────────────────────────
  document.getElementById('confirmRevokeBtn')!.addEventListener('click', confirmRevoke);
  document.getElementById('enrollPrev')!.addEventListener('click', () => loadEnrollments(enrollPage - 1));
  document.getElementById('enrollNext')!.addEventListener('click', () => loadEnrollments(enrollPage + 1));

  document.querySelectorAll<HTMLElement>('#enrollStatusTabs .nav-link').forEach(tab => {
    tab.addEventListener('click', (e: Event) => {
      e.preventDefault();
      document.querySelectorAll('#enrollStatusTabs .nav-link').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      enrollStatus = tab.dataset.status!;
      const title = document.getElementById('enrollTableTitle');
      if (title) title.textContent = enrollStatusTitles[enrollStatus] ?? 'Đăng ký';
      loadEnrollments(0);
    });
  });

  // ── Moodle Integration ─────────────────────────────────────
  document.getElementById('moodleCheckStatusBtn')?.addEventListener('click', async () => {
    const resultEl = document.getElementById('moodleStatusResult');
    if (!resultEl) return;
    resultEl.className = 'mt-2 small text-muted';
    resultEl.textContent = 'Đang kiểm tra...';
    resultEl.classList.remove('d-none');
    try {
      const info = await apiGet<Record<string, unknown>>('/admin/moodle/status');
      resultEl.className = 'mt-2 small text-success';
      resultEl.textContent = `✓ Kết nối thành công — ${info.sitename ?? 'Moodle'} (v${info.release ?? '?'})`;
    } catch {
      resultEl.className = 'mt-2 small text-danger';
      resultEl.textContent = '✗ Không thể kết nối Moodle. Kiểm tra cấu hình MOODLE_URL và MOODLE_TOKEN.';
    }
  });
  document.getElementById('moodleSyncCoursesBtn')?.addEventListener('click', async () => {
    const btn = document.getElementById('moodleSyncCoursesBtn') as HTMLButtonElement;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang đồng bộ...';
    try {
      const res = await apiPost<{ synced: number }>('/admin/moodle/sync-courses', {});
      toast.success(`Đã đồng bộ ${res.synced} khóa học mới lên Moodle.`);
    } catch {
      toast.error('Đồng bộ khóa học thất bại.');
    }
    btn.disabled = false;
    btn.innerHTML = '<i class="fa fa-book me-1"></i>Đồng bộ khóa học → Moodle';
  });
  document.getElementById('moodleSyncUsersBtn')?.addEventListener('click', async () => {
    const btn = document.getElementById('moodleSyncUsersBtn') as HTMLButtonElement;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang đồng bộ...';
    try {
      const res = await apiPost<{ synced: number }>('/admin/moodle/sync-users', {});
      toast.success(`Đã đồng bộ ${res.synced} người dùng mới lên Moodle.`);
    } catch {
      toast.error('Đồng bộ người dùng thất bại.');
    }
    btn.disabled = false;
    btn.innerHTML = '<i class="fa fa-users me-1"></i>Đồng bộ người dùng → Moodle';
  });

  // ── Load dashboard stats immediately ─────────────────────────
  loadStats();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
