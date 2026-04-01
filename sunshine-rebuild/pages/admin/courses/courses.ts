import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import '../admin.css';
import 'bootstrap';
import { Modal } from 'bootstrap';

import { initI18n } from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { apiGet, apiPost, apiPut, apiDelete } from '../../../src/shared/js/api.ts';
import { toast } from '../../../src/shared/js/toast.ts';
import { requireAdmin, formatDate } from '../admin-utils.ts';
import type { CourseResponse, Page, UserResponse, MoodleLaunchResponse } from '../../../src/shared/js/types.ts';

let currentPage      = 0;
let totalPages       = 0;
let currentKeyword   = '';
let currentPublished = '';
let currentCategory  = '';
let courseModalInst: InstanceType<typeof Modal> | null  = null;
let isEditMode       = false;

function renderRow(c: CourseResponse): string {
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
        data-teachername="${escAttr(c.teacherName || '')}"
        data-published="${c.published}"
        title="Chỉnh sửa"><i class="fa fa-pen"></i></button>
      ${c.moodleCourseId ? `<button class="btn btn-sm btn-outline-warning py-0 me-1" data-action="teacher-launch" data-id="${c.id}" title="Soạn giáo án trên Moodle"><i class="fa fa-pen-to-square"></i></button>` : ''}
      <button class="btn btn-sm btn-outline-danger py-0" data-action="delete" data-id="${c.id}" data-name="${escAttr(c.name)}" title="Xóa"><i class="fa fa-trash"></i></button>
    </td>
  </tr>`;
}

function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
function escAttr(s: string | null | undefined): string {
  return String(s ?? '').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

async function loadCourses(page: number = 0): Promise<void> {
  const tbody = document.getElementById('courseTableBody')!;
  tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';

  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (currentKeyword)   params.set('keyword', currentKeyword);
  if (currentPublished) params.set('published', currentPublished);
  if (currentCategory)  params.set('category', currentCategory);

  try {
    const data = await apiGet<Page<CourseResponse>>(`/admin/courses?${params.toString()}`);
    currentPage = data.number;
    totalPages  = data.totalPages;

    tbody.innerHTML = data.content.length === 0
      ? '<tr><td colspan="7" class="text-center text-muted py-4">Không có khóa học.</td></tr>'
      : data.content.map(renderRow).join('');

    tbody.querySelectorAll('[data-action]').forEach(btn => btn.addEventListener('click', handleAction));

    const pi = document.getElementById('pageInfo');
    if (pi) pi.textContent = `Trang ${currentPage + 1} / ${totalPages || 1} (${data.totalElements ?? 0} mục)`;
    (document.getElementById('prevPage') as HTMLButtonElement).disabled = currentPage === 0;
    (document.getElementById('nextPage') as HTMLButtonElement).disabled = currentPage >= totalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

function handleAction(e: Event): void {
  const btn    = e.currentTarget as HTMLElement;
  const action = btn.dataset.action;
  if (action === 'edit')           openEditModal(btn.dataset as Record<string, string>);
  if (action === 'delete')         deleteCourse(Number(btn.dataset.id), btn.dataset.name!);
  if (action === 'teacher-launch') launchTeacherMoodle(btn);
}

async function launchTeacherMoodle(btn: HTMLElement): Promise<void> {
  const courseId = btn.dataset.id;
  if (!courseId) return;
  const origHtml = btn.innerHTML;
  btn.classList.add('disabled');
  btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
  try {
    const res = await apiGet<MoodleLaunchResponse>(`/moodle/teacher-launch?courseId=${courseId}`);
    window.open(res.url, '_blank', 'noopener,noreferrer');
  } catch {
    toast.error('Không thể mở Moodle. Vui lòng thử lại sau.');
  } finally {
    btn.classList.remove('disabled');
    btn.innerHTML = origHtml;
  }
}

function clearModal(): void {
  ['courseId','courseName','courseCategory','courseDescription'].forEach(id => {
    const el = document.getElementById(id) as HTMLInputElement | null;
    if (el) el.value = '';
  });
  (document.getElementById('courseImageUrl') as HTMLInputElement).value = '';
  // Clear teacher search controls
  const teacherIdEl = document.getElementById('courseTeacherId') as HTMLInputElement | null;
  if (teacherIdEl) teacherIdEl.value = '';
  const searchEl = document.getElementById('courseTeacherSearch') as HTMLInputElement | null;
  if (searchEl) searchEl.value = '';
  document.getElementById('teacherDropdown')?.classList.add('d-none');
  document.getElementById('teacherSelected')?.classList.add('d-none');
  const nameEl = document.getElementById('teacherSelectedName');
  if (nameEl) nameEl.textContent = '';
  (document.getElementById('courseLevel') as HTMLSelectElement).value     = '';
  (document.getElementById('coursePublished') as HTMLInputElement).checked = true;
  document.getElementById('courseModalAlert')!.className = 'alert d-none';
}

function openCreateModal(): void {
  isEditMode = false;
  clearModal();
  document.getElementById('courseModalTitle')!.textContent = 'Thêm khóa học';
  courseModalInst?.show();
}

function openEditModal(d: Record<string, string>): void {
  isEditMode = true;
  clearModal();
  document.getElementById('courseModalTitle')!.textContent  = 'Chỉnh sửa khóa học';
  (document.getElementById('courseId') as HTMLInputElement).value                = d.id;
  (document.getElementById('courseName') as HTMLInputElement).value              = d.name;
  (document.getElementById('courseCategory') as HTMLInputElement).value          = d.category || '';
  (document.getElementById('courseLevel') as HTMLSelectElement).value             = d.level || '';
  (document.getElementById('courseDescription') as HTMLTextAreaElement).value       = d.description || '';
  (document.getElementById('courseImageUrl') as HTMLInputElement).value          = d.imageurl || '';
  (document.getElementById('coursePublished') as HTMLInputElement).checked       = d.published === 'true';
  // Pre-fill teacher if assigned
  if (d.teacherid && d.teachername) {
    (document.getElementById('courseTeacherId') as HTMLInputElement).value = d.teacherid;
    const search = document.getElementById('courseTeacherSearch') as HTMLInputElement;
    if (search) search.value = '';
    const nameEl = document.getElementById('teacherSelectedName');
    if (nameEl) nameEl.textContent = d.teachername;
    document.getElementById('teacherSelected')?.classList.remove('d-none');
  }
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
  const payload = {
    name,
    category:    (document.getElementById('courseCategory') as HTMLInputElement).value.trim() || null,
    level:       (document.getElementById('courseLevel') as HTMLSelectElement).value || null,
    description: (document.getElementById('courseDescription') as HTMLTextAreaElement).value.trim() || null,
    imageUrl:    (document.getElementById('courseImageUrl') as HTMLInputElement).value.trim() || null,
    teacherId:   Number((document.getElementById('courseTeacherId') as HTMLInputElement).value) || null,
    published:   (document.getElementById('coursePublished') as HTMLInputElement).checked,
  };
  saveBtn.disabled = true;
  try {
    if (isEditMode) {
      const id = (document.getElementById('courseId') as HTMLInputElement).value;
      await apiPut(`/admin/courses/${id}`, payload);
    } else {
      await apiPost('/admin/courses', payload);
    }
    courseModalInst?.hide();
    loadCourses(currentPage);
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
    loadCourses(currentPage);
  } catch (err: any) {
    toast.error(err.message);
  }
}

/* ── Teacher typeahead search ───────────────────────────────── */
let teacherSearchTimer: ReturnType<typeof setTimeout> | null = null;

function selectTeacher(id: number, displayName: string): void {
  (document.getElementById('courseTeacherId') as HTMLInputElement).value = String(id);
  (document.getElementById('courseTeacherSearch') as HTMLInputElement).value = '';
  const nameEl = document.getElementById('teacherSelectedName');
  if (nameEl) nameEl.textContent = displayName;
  document.getElementById('teacherSelected')?.classList.remove('d-none');
  document.getElementById('teacherDropdown')?.classList.add('d-none');
}

function clearTeacher(): void {
  (document.getElementById('courseTeacherId') as HTMLInputElement).value = '';
  (document.getElementById('courseTeacherSearch') as HTMLInputElement).value = '';
  document.getElementById('teacherSelected')?.classList.add('d-none');
  const nameEl = document.getElementById('teacherSelectedName');
  if (nameEl) nameEl.textContent = '';
}

function initTeacherSearch(): void {
  const searchEl  = document.getElementById('courseTeacherSearch') as HTMLInputElement;
  const dropdown  = document.getElementById('teacherDropdown')!;
  const clearBtn  = document.getElementById('clearTeacherBtn');

  clearBtn?.addEventListener('click', clearTeacher);

  searchEl.addEventListener('input', () => {
    const q = searchEl.value.trim();
    if (teacherSearchTimer) clearTimeout(teacherSearchTimer);
    if (q.length < 1) { dropdown.classList.add('d-none'); return; }

    teacherSearchTimer = setTimeout(async () => {
      try {
        const data = await apiGet<Page<UserResponse>>(`/admin/users?role=TEACHER&keyword=${encodeURIComponent(q)}&page=0&size=10`);
        if (!data.content || data.content.length === 0) {
          dropdown.innerHTML = '<div class="list-group-item list-group-item-action disabled text-muted small">Không tìm thấy giáo viên</div>';
          dropdown.classList.remove('d-none');
          return;
        }
        dropdown.innerHTML = data.content.map(u => {
          const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.username;
          const label = name !== u.username ? `${name} <span class="text-muted">(${escHtml(u.username)})</span>` : escHtml(u.username);
          return `<button type="button" class="list-group-item list-group-item-action small py-2"
                    data-teacher-id="${u.id}" data-teacher-name="${escAttr(name !== u.username ? name + ' (@' + u.username + ')' : u.username)}">${label}</button>`;
        }).join('');
        dropdown.classList.remove('d-none');
        dropdown.querySelectorAll('[data-teacher-id]').forEach(btn => {
          btn.addEventListener('click', () => {
            const tid   = Number((btn as HTMLElement).dataset.teacherId);
            const tname = (btn as HTMLElement).dataset.teacherName || '';
            selectTeacher(tid, tname);
          });
        });
      } catch {
        dropdown.classList.add('d-none');
      }
    }, 300);
  });

  // Close dropdown when clicking outside
  document.addEventListener('click', (e) => {
    if (!searchEl.contains(e.target as Node) && !dropdown.contains(e.target as Node)) {
      dropdown.classList.add('d-none');
    }
  });
}

function initApp(): void {
  requireAdmin();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();

  courseModalInst = new Modal(document.getElementById('courseModal')!);
  document.getElementById('addCourseBtn')!.addEventListener('click', openCreateModal);
  document.getElementById('saveCourseBtn')!.addEventListener('click', saveCourse);
  document.getElementById('prevPage')!.addEventListener('click', () => loadCourses(currentPage - 1));
  document.getElementById('nextPage')!.addEventListener('click', () => loadCourses(currentPage + 1));

  document.getElementById('applyFilter')!.addEventListener('click', () => {
    currentKeyword   = (document.getElementById('courseSearch') as HTMLInputElement).value.trim();
    currentPublished = (document.getElementById('publishedFilter') as HTMLSelectElement).value;
    currentCategory  = (document.getElementById('categoryFilter') as HTMLInputElement).value.trim();
    loadCourses(0);
  });
  document.getElementById('courseSearch')!.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      currentKeyword = (e.target as HTMLInputElement).value.trim();
      loadCourses(0);
    }
  });

  initTeacherSearch();
  loadCourses(0);
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
