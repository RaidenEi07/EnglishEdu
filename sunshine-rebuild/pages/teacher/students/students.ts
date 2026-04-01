import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import '../teacher.css';
import 'bootstrap';
import { Modal } from 'bootstrap';

import { initI18n }   from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { apiGet, apiPatch } from '../../../src/shared/js/api.ts';
import { toast } from '../../../src/shared/js/toast.ts';
import { requireTeacher, formatDate, statusBadge, progressBar } from '../teacher-utils.ts';
import type { TeacherEnrollmentResponse, CourseResponse, Page } from '../../../src/shared/js/types.ts';

// ── state ─────────────────────────────────────────────────────────────────────
let currentPage    = 0;
let totalPages     = 0;
let editModalInst: InstanceType<typeof Modal> | null  = null;
let editingId: number | null      = null;

// ── helpers ────────────────────────────────────────────────────────────────
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function getFilters(): { courseId: string; status: string } {
  return {
    courseId: (document.getElementById('filterCourse') as HTMLSelectElement).value.trim(),
    status:   (document.getElementById('filterStatus') as HTMLSelectElement).value.trim(),
  };
}

// ── courses dropdown ──────────────────────────────────────────────────────────
async function populateCourseFilter(preselect: string): Promise<void> {
  const sel = document.getElementById('filterCourse') as HTMLSelectElement;
  try {
    const data = await apiGet<Page<CourseResponse>>('/teacher/courses?page=0&size=100');
    (data.content ?? []).forEach((c) => {
      const opt = document.createElement('option');
      opt.value = String(c.id);
      opt.textContent = c.name;
      if (String(c.id) === String(preselect)) opt.selected = true;
      sel.appendChild(opt);
    });
  } catch (err) {
    console.error('Failed to load courses for filter', err);
  }
}

// ── table ─────────────────────────────────────────────────────────────────────
function renderRow(e: TeacherEnrollmentResponse): string {
  const fullName = [e.studentFirstName, e.studentLastName].filter(Boolean).join(' ') || e.studentUsername;
  return `<tr>
    <td>
      <div class="fw-medium">${escHtml(fullName)}</div>
      <div class="text-muted small">${escHtml(e.studentUsername)}</div>
    </td>
    <td class="d-none d-md-table-cell small text-truncate" style="max-width:200px">${escHtml(e.courseName)}</td>
    <td>${statusBadge(e.status)}</td>
    <td>${progressBar(e.progress)}</td>
    <td class="d-none d-lg-table-cell small">${formatDate(e.enrolledAt)}</td>
    <td class="d-none d-lg-table-cell small">${formatDate(e.expiryDate)}</td>
    <td>
      <button class="btn btn-sm btn-outline-success py-0" data-action="edit" data-id="${e.id}"
        title="Chỉnh sửa">
        <i class="fa fa-pen"></i>
      </button>
    </td>
  </tr>`;
}

async function loadEnrollments(page: number = 0): Promise<void> {
  const tbody = document.getElementById('enrollTableBody')!;
  tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';

  const { courseId, status } = getFilters();
  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (courseId) params.set('courseId', courseId);
  if (status)   params.set('status',   status);

  try {
    const data = await apiGet<Page<TeacherEnrollmentResponse>>(`/teacher/enrollments?${params}`);
    currentPage = data.number;
    totalPages  = data.totalPages;

    const badge = document.getElementById('totalBadge');
    if (badge) badge.textContent = String(data.totalElements);

    if (!data.content?.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Không có học viên nào</td></tr>';
      renderPagination(0, 0);
      return;
    }
    tbody.innerHTML = data.content.map(renderRow).join('');
    renderPagination(currentPage, totalPages);
  } catch (err) {
    console.error('Failed to load enrollments', err);
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4"><i class="fa fa-triangle-exclamation me-2"></i>Lỗi tải dữ liệu</td></tr>';
  }
}

function renderPagination(page: number, total: number): void {
  const wrap = document.getElementById('paginationWrap');
  if (!wrap || total <= 1) { if (wrap) wrap.innerHTML = ''; return; }
  const pages = [];
  for (let i = 0; i < total; i++) {
    pages.push(`<li class="page-item ${i === page ? 'active' : ''}">
      <button class="page-link" data-page="${i}">${i + 1}</button>
    </li>`);
  }
  wrap.innerHTML = `<nav><ul class="pagination pagination-sm mb-0">${pages.join('')}</ul></nav>`;
  wrap.addEventListener('click', (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>('[data-page]');
    if (btn) loadEnrollments(Number(btn.dataset.page));
  });
}

// ── edit modal ────────────────────────────────────────────────────────────────
function openEditModal(enrollmentId: number): void {
  editingId = enrollmentId;
  // Find row data from the DOM click target rather than fetching again
  const row = document.querySelector(`[data-action="edit"][data-id="${enrollmentId}"]`)?.closest('tr');
  if (!row) return;

  const cells = row.querySelectorAll('td');
  const fullInfo = cells[0]?.innerHTML || '';
  document.getElementById('editStudentInfo')!.innerHTML = fullInfo;

  // Pre-fill status from badge text
  const badgeText = cells[2]?.querySelector('.badge')?.textContent?.trim() || '';
  const statusSel = document.getElementById('editStatus') as HTMLSelectElement;
  [...statusSel.options].forEach((opt) => {
    if (opt.value === badgeText) opt.selected = true;
  });

  // Pre-fill progress (parse from progress bar aria or text)
  const progressText = cells[3]?.querySelector('small')?.textContent?.replace('%', '').trim();
  const pct = parseInt(progressText || '0', 10);
  (document.getElementById('editProgress') as HTMLInputElement).value = String(pct);
  document.getElementById('editProgressLabel')!.textContent = String(pct);

  // Clear note & expiry (server doesn't return these in table, user may retype)
  (document.getElementById('editNote') as HTMLTextAreaElement).value   = '';
  (document.getElementById('editExpiry') as HTMLInputElement).value = '';

  editModalInst!.show();
}

async function saveEdit(): Promise<void> {
  if (!editingId) return;
  const spinner = document.getElementById('saveEditSpinner')!;
  const icon    = document.getElementById('saveEditIcon')!;
  spinner.classList.remove('d-none');
  icon.classList.add('d-none');

  const payload = {
    status:    (document.getElementById('editStatus') as HTMLSelectElement).value   || undefined,
    progress:  Number((document.getElementById('editProgress') as HTMLInputElement).value),
    teacherNote: (document.getElementById('editNote') as HTMLTextAreaElement).value.trim() || undefined,
    expiryDate:  (document.getElementById('editExpiry') as HTMLInputElement).value  || undefined,
  };

  try {
    await apiPatch(`/teacher/enrollments/${editingId}`, payload);
    editModalInst?.hide();
    loadEnrollments(currentPage);
  } catch (err: any) {
    console.error('Failed to save enrollment update', err);
    toast.error('Lỗi lưu dữ liệu: ' + (err?.message || 'Vui lòng thử lại'));
  } finally {
    spinner.classList.add('d-none');
    icon.classList.remove('d-none');
  }
}

// ── init ──────────────────────────────────────────────────────────────────
function bindEvents(): void {
  // Progress slider label update
  document.getElementById('editProgress')!.addEventListener('input', (e: Event) => {
    document.getElementById('editProgressLabel')!.textContent = (e.target as HTMLInputElement).value;
  });

  // Table action buttons (delegated)
  document.getElementById('enrollTableBody')!.addEventListener('click', (e: Event) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>('[data-action="edit"]');
    if (btn) openEditModal(Number(btn.dataset.id));
  });

  // Filter changes
  document.getElementById('filterCourse')!.addEventListener('change', () => loadEnrollments(0));
  document.getElementById('filterStatus')!.addEventListener('change', () => loadEnrollments(0));

  // Save modal
  document.getElementById('saveEditBtn')!.addEventListener('click', saveEdit);
}

function initApp(): void {
  requireTeacher();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();

  editModalInst = new Modal(document.getElementById('editModal')!);

  // Pre-select courseId from URL eg. ?courseId=5
  const params    = new URLSearchParams(window.location.search);
  const courseId  = params.get('courseId') || '';
  const statusParam = params.get('status') || '';

  if (statusParam) {
    const statusSel = document.getElementById('filterStatus') as HTMLSelectElement;
    [...statusSel.options].forEach((opt) => {
      if (opt.value === statusParam) opt.selected = true;
    });
  }

  bindEvents();
  populateCourseFilter(courseId).then(() => loadEnrollments(0));
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
