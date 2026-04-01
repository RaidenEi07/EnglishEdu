import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import '../admin.css';
import 'bootstrap';
import { Modal } from 'bootstrap';

import { initI18n } from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { apiGet, apiPost, apiPatch, apiDelete } from '../../../src/shared/js/api.ts';
import { toast } from '../../../src/shared/js/toast.ts';
import { requireAdmin, formatDate } from '../admin-utils.ts';
import type { UserResponse, Page, CreateAdminUserRequest, CourseResponse, CourseAssignmentResponse, AssignCoursesRequest } from '../../../src/shared/js/types.ts';

let currentPage = 0;
let totalPages  = 0;
let currentRole = '';
let currentKeyword = '';
let roleModalInstance: InstanceType<typeof Modal> | null = null;
let createUserModalInstance: InstanceType<typeof Modal> | null = null;
let assignModalInstance: InstanceType<typeof Modal> | null = null;
let editingUserId: number | null = null;
let assigningUserId: number | null = null;
let assigningUsername: string = '';
let allCoursesCache: CourseResponse[] = [];
let searchTimer: ReturnType<typeof setTimeout> | null = null;

function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function roleBadge(role: string): string {
  const map: Record<string, string> = { ADMIN: 'bg-danger', TEACHER: 'bg-success', STUDENT: 'bg-primary' };
  return `<span class="badge ${map[role] || 'bg-secondary'}">${role}</span>`;
}

function renderRow(u: UserResponse): string {
  const active = u.active
    ? '<span class="badge bg-success-subtle text-success">Hoạt động</span>'
    : '<span class="badge bg-secondary">Tắt</span>';
  return `<tr>
    <td class="text-muted small">${u.id}</td>
    <td><span class="fw-medium">${escHtml(u.username)}</span></td>
    <td class="d-none d-md-table-cell">${escHtml(u.firstName || '')} ${escHtml(u.lastName || '')}</td>
    <td class="d-none d-lg-table-cell small">${escHtml(u.email)}</td>
    <td>${roleBadge(u.role)}</td>
    <td>${active}</td>
    <td class="d-none d-lg-table-cell small">${formatDate(u.createdAt)}</td>
    <td>
      <button class="btn btn-xs btn-outline-secondary btn-sm py-0 me-1" data-action="role" data-id="${u.id}" data-username="${u.username}" data-role="${u.role}" title="Đổi vai trò">
        <i class="fa fa-user-tag"></i>
      </button>
      ${u.role === 'STUDENT' ? `<button class="btn btn-xs btn-outline-primary btn-sm py-0 me-1" data-action="assign" data-id="${u.id}" data-username="${u.username}" title="Chỉ định khóa học"><i class="fa fa-book-open"></i></button>` : ''}
      <button class="btn btn-xs ${u.active ? 'btn-outline-warning' : 'btn-outline-success'} btn-sm py-0" data-action="toggle" data-id="${u.id}" title="${u.active ? 'Vô hiệu hóa' : 'Kích hoạt'}">
        <i class="fa ${u.active ? 'fa-ban' : 'fa-check'}"></i>
      </button>
    </td>
  </tr>`;
}

async function loadUsers(page: number = 0): Promise<void> {
  const tbody = document.getElementById('userTableBody')!;
  tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';

  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (currentRole)    params.set('role', currentRole);
  if (currentKeyword) params.set('keyword', currentKeyword);

  try {
    const data = await apiGet<Page<UserResponse>>(`/admin/users?${params.toString()}`);
    currentPage = data.number;
    totalPages  = data.totalPages;

    if (data.content.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Không tìm thấy người dùng nào.</td></tr>';
    } else {
      tbody.innerHTML = data.content.map(renderRow).join('');
      tbody.querySelectorAll('[data-action]').forEach(btn => btn.addEventListener('click', handleAction));
    }

    const pageInfo = document.getElementById('pageInfo');
    if (pageInfo) pageInfo.textContent = `Trang ${currentPage + 1} / ${totalPages || 1} (${data.totalElements ?? 0} mục)`;

    (document.getElementById('prevPage') as HTMLButtonElement).disabled = currentPage === 0;
    (document.getElementById('nextPage') as HTMLButtonElement).disabled = currentPage >= totalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

function handleAction(e: Event): void {
  const btn    = e.currentTarget as HTMLElement;
  const action = btn.dataset.action;
  const id     = Number(btn.dataset.id);

  if (action === 'role') {
    editingUserId = id;
    document.getElementById('roleModalUsername')!.textContent = btn.dataset.username!;
    (document.getElementById('roleSelect') as HTMLSelectElement).value = btn.dataset.role!;
    roleModalInstance?.show();
  } else if (action === 'toggle') {
    toggleActive(id, btn as HTMLButtonElement);
  } else if (action === 'assign') {
    openAssignModal(id, btn.dataset.username || '');
  }
}

async function toggleActive(id: number, btn: HTMLButtonElement): Promise<void> {
  btn.disabled = true;
  try {
    await apiPatch(`/admin/users/${id}/active`);
    loadUsers(currentPage);
  } catch (err: any) {
    toast.error(err.message);
    btn.disabled = false;
  }
}

async function saveRole(): Promise<void> {
  if (!editingUserId) return;
  const role = (document.getElementById('roleSelect') as HTMLSelectElement).value;
  const saveBtn = document.getElementById('saveRoleBtn') as HTMLButtonElement;
  saveBtn.disabled = true;
  try {
    await apiPatch(`/admin/users/${editingUserId}/role`, { role });
    roleModalInstance?.hide();
    loadUsers(currentPage);
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
  const alert = document.getElementById('cuAlert')!;
  alert.className = 'alert d-none';
  alert.textContent = '';
}

/* ── Course Assignment Modal ──────────────────────────────────────────────── */
async function loadAllCourses(): Promise<CourseResponse[]> {
  if (allCoursesCache.length > 0) return allCoursesCache;
  const page = await apiGet<Page<CourseResponse>>('/courses?size=100');
  allCoursesCache = page.content || (page as unknown as CourseResponse[]);
  return allCoursesCache;
}

async function openAssignModal(userId: number, username: string): Promise<void> {
  assigningUserId = userId;
  assigningUsername = username;
  document.getElementById('assignModalUsername')!.textContent = username;

  const listEl = document.getElementById('assignCourseList')!;
  listEl.innerHTML = '<div class="text-center py-3"><span class="spinner-border spinner-border-sm"></span></div>';
  assignModalInstance?.show();

  try {
    const [courses, current] = await Promise.all([
      loadAllCourses(),
      apiGet<CourseAssignmentResponse[]>(`/admin/course-assignments?userId=${userId}`),
    ]);
    const assignedIds = new Set(current.map(a => a.courseId));

    listEl.innerHTML = courses.map(c => `
      <div class="form-check mb-1">
        <input class="form-check-input" type="checkbox" value="${c.id}" id="ac-${c.id}" ${assignedIds.has(c.id) ? 'checked' : ''}>
        <label class="form-check-label small" for="ac-${c.id}">
          <span class="badge bg-secondary me-1">${c.category}</span>${c.name}
        </label>
      </div>`).join('');
  } catch (err: any) {
    listEl.innerHTML = `<div class="text-danger small">${escHtml(err.message)}</div>`;
  }
}

async function saveAssignments(): Promise<void> {
  if (!assigningUserId) return;
  const saveBtn = document.getElementById('saveAssignBtn') as HTMLButtonElement;
  saveBtn.disabled = true;

  const checkboxes = document.querySelectorAll<HTMLInputElement>('#assignCourseList .form-check-input');
  const selectedIds: number[] = [];
  checkboxes.forEach(cb => { if (cb.checked) selectedIds.push(Number(cb.value)); });

  try {
    // Get current assignments
    const current = await apiGet<CourseAssignmentResponse[]>(`/admin/course-assignments?userId=${assigningUserId}`);
    const currentIds = new Set(current.map(a => a.courseId));

    // Courses to add
    const toAdd = selectedIds.filter(id => !currentIds.has(id));
    // Courses to remove
    const toRemove = [...currentIds].filter(id => !selectedIds.includes(id));

    if (toAdd.length > 0) {
      const payload: AssignCoursesRequest = { userId: assigningUserId, courseIds: toAdd };
      await apiPost('/admin/course-assignments', payload);
    }
    for (const courseId of toRemove) {
      await apiDelete(`/admin/course-assignments?userId=${assigningUserId}&courseId=${courseId}`);
    }

    assignModalInstance?.hide();
    toast.success(`Đã cập nhật khóa học cho "${assigningUsername}".`);
  } catch (err: any) {
    toast.error(err.message);
  } finally {
    saveBtn.disabled = false;
  }
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
    createUserModalInstance?.hide();
    toast.success(`Tài khoản "${username}" đã được tạo thành công.`);
    loadUsers(0);
  } catch (err: any) {
    alertEl.className   = 'alert alert-danger';
    alertEl.textContent = err.message;
  } finally {
    saveBtn.disabled = false;
  }
}

function initApp(): void {
  requireAdmin();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();

  roleModalInstance       = new Modal(document.getElementById('roleModal')!);
  createUserModalInstance = new Modal(document.getElementById('createUserModal')!);
  assignModalInstance     = new Modal(document.getElementById('assignModal')!);

  document.getElementById('saveRoleBtn')!.addEventListener('click', saveRole);
  document.getElementById('saveNewUserBtn')!.addEventListener('click', saveNewUser);
  document.getElementById('saveAssignBtn')!.addEventListener('click', saveAssignments);
  document.getElementById('createUserBtn')!.addEventListener('click', () => {
    clearCreateUserForm();
    createUserModalInstance?.show();
  });

  document.getElementById('applyFilter')!.addEventListener('click', () => {
    currentRole    = (document.getElementById('roleFilter') as HTMLSelectElement).value;
    currentKeyword = (document.getElementById('searchInput') as HTMLInputElement).value.trim();
    loadUsers(0);
  });

  // Debounced server-side search (500 ms) or immediate on Enter
  document.getElementById('searchInput')!.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      if (searchTimer) clearTimeout(searchTimer);
      currentKeyword = (e.target as HTMLInputElement).value.trim();
      loadUsers(0);
    }
  });
  document.getElementById('searchInput')!.addEventListener('input', (e: Event) => {
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      currentKeyword = (e.target as HTMLInputElement).value.trim();
      loadUsers(0);
    }, 500);
  });

  document.getElementById('prevPage')!.addEventListener('click', () => loadUsers(currentPage - 1));
  document.getElementById('nextPage')!.addEventListener('click', () => loadUsers(currentPage + 1));

  loadUsers(0);
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
