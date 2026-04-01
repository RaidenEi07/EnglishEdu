/**
 * My Courses page entry point (/pages/my/courses/)
 */

/* --- Styles --- */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import './courses.css';

/* --- Bootstrap JS --- */
import 'bootstrap';
import { Modal } from 'bootstrap';

/* --- Shared modules --- */
import { initI18n } from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { isLoggedIn } from '../../../src/shared/js/auth.ts';
import { apiPatch } from '../../../src/shared/js/api.ts';
import { toast } from '../../../src/shared/js/toast.ts';
import type { EnrolledCourseResponse, EnrollmentStatus } from '../../../src/shared/js/types.ts';

/* --- Section utilities shared with dashboard page --- */
import { fetchMyCourses, setUserInitials } from '../my-utils.ts';

// ── helpers ──────────────────────────────────────────────────────────────────
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function statusBadge(status: EnrollmentStatus | string | null | undefined): string {
  const map: Record<string, [string, string]> = {
    pending:    ['bg-warning text-dark', 'Chờ duyệt'],
    active:     ['bg-success',           'Đã duyệt'],
    inprogress: ['bg-primary',           'Đang học'],
    completed:  ['bg-info text-dark',    'Hoàn thành'],
    revoked:    ['bg-danger',            'Đã thu hồi'],
  };
  const [cls, label] = map[status as string] || ['bg-secondary', status ?? '—'];
  return `<span class="badge ${cls}">${label}</span>`;
}

function ctaButton(course: EnrolledCourseResponse): string {
  const id = course.courseId;
  if (course.status === 'pending') {
    return `<button class="btn btn-warning btn-sm w-100" disabled><i class="fa fa-clock me-1"></i>Chờ duyệt</button>`;
  }
  if (course.status === 'revoked') {
    return `<button class="btn btn-danger btn-sm w-100" disabled><i class="fa fa-ban me-1"></i>Đã thu hồi</button>`;
  }
  if (course.status === 'completed') {
    return `<a href="/pages/learn/?id=${id}" class="btn btn-secondary btn-sm w-100"><i class="fa fa-check me-1"></i>Ôn tập</a>`;
  }
  // active or inprogress
  const label = (course.progress ?? 0) > 0 ? 'Tiếp tục học' : 'Bắt đầu học';
  return `<a href="/pages/learn/?id=${id}" class="btn btn-primary btn-sm w-100">${label}</a>`;
}

function progressUpdateBtn(course: EnrolledCourseResponse): string {
  if (['pending', 'revoked'].includes(course.status)) return '';
  const id = course.courseId ?? course.enrollmentId;
  return `<button class="btn btn-outline-secondary btn-sm px-2 ms-1 update-progress-btn" title="Cập nhật tiến độ"
    data-enrollment-id="${course.enrollmentId}"
    data-course-id="${course.courseId}"
    data-course-name="${escHtml(course.name)}"
    data-progress="${course.progress ?? 0}">
    <i class="fa fa-chart-line"></i>
  </button>`;
}

/* ── Course card renderers ─────────────────────────────────── */
function renderCourseCard(course: EnrolledCourseResponse): string {
  const progress = course.progress ?? 0;
  const teacherNote = course.teacherNote
    ? `<div class="alert alert-info py-1 px-2 small mt-2 mb-0"><i class="fa fa-comment me-1"></i>${escHtml(course.teacherNote)}</div>`
    : '';
  return `
    <div class="col-lg-3 col-md-4 col-sm-6 my-course-item">
      <div class="card h-100 border-0 shadow-sm">
        <img src="${escHtml(course.imageUrl || '/images/course-placeholder.svg')}"
             class="card-img-top" alt="${escHtml(course.name)}" loading="lazy" style="height:150px;object-fit:cover;">
        <div class="card-body">
          <div class="d-flex justify-content-between mb-1 align-items-start gap-1">
            <small class="text-muted">${escHtml(course.category || '')}</small>
            ${statusBadge(course.status)}
          </div>
          <h6 class="card-title">${escHtml(course.name)}</h6>
          <div class="progress mb-1" style="height:6px;">
            <div class="progress-bar bg-success" style="width:${progress}%" role="progressbar"></div>
          </div>
          <small class="text-muted">${progress}%</small>
          ${teacherNote}
        </div>
        <div class="card-footer bg-transparent border-0 d-flex gap-1">
          <div class="flex-grow-1">${ctaButton(course)}</div>
          ${progressUpdateBtn(course)}
        </div>
      </div>
    </div>`;
}

function renderCourseRow(course: EnrolledCourseResponse): string {
  const progress = course.progress ?? 0;
  return `
    <div class="col-12 my-course-item">
      <div class="card border-0 shadow-sm">
        <div class="card-body d-flex align-items-center gap-3">
          <img src="${escHtml(course.imageUrl || '/images/course-placeholder.svg')}"
               alt="${escHtml(course.name)}" class="rounded" width="80" height="56" style="object-fit:cover" loading="lazy">
          <div class="flex-grow-1">
            <div class="fw-medium">${escHtml(course.name)}</div>
            <div class="d-flex align-items-center gap-2">
              <small class="text-muted">${escHtml(course.category || '')}</small>
              ${statusBadge(course.status)}
            </div>
            <div class="progress mt-1" style="height:4px;max-width:200px;">
              <div class="progress-bar bg-success" style="width:${progress}%"></div>
            </div>
            ${course.teacherNote ? `<small class="text-info"><i class="fa fa-comment me-1"></i>${escHtml(course.teacherNote)}</small>` : ''}
          </div>
          <div class="text-muted small text-end" style="min-width:60px">${progress}%</div>
          <div class="d-flex gap-1">
            ${ctaButton(course)}
            ${progressUpdateBtn(course)}
          </div>
        </div>
      </div>
    </div>`;
}

function renderCourseSummary(course: EnrolledCourseResponse): string {
  const progress = course.progress ?? 0;
  return `
    <div class="col-12 my-course-item">
      <div class="card border-0 shadow-sm mb-2">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-start">
            <h6 class="mb-1"><a href="/pages/learn/?id=${course.courseId}" class="text-decoration-none">${escHtml(course.name)}</a></h6>
            ${statusBadge(course.status)}
          </div>
          <small class="text-muted d-block mb-2">${escHtml(course.category || '')}</small>
          <div class="progress" style="height:4px;">
            <div class="progress-bar bg-success" style="width:${progress}%"></div>
          </div>
          ${course.teacherNote ? `<small class="text-info mt-1 d-block"><i class="fa fa-comment me-1"></i>${escHtml(course.teacherNote)}</small>` : ''}
        </div>
      </div>
    </div>`;
}

// ── Progress update modal ─────────────────────────────────────────────────────
declare const bootstrap: any;
let progressModal: InstanceType<typeof Modal> | null    = null;
let progressCourseId: number | string | null = null;

function initProgressModal(): void {
  const modalEl = document.getElementById('progressModal');
  if (!modalEl) return;
  progressModal = new Modal(modalEl);

  const slider  = document.getElementById('progressModalSlider') as HTMLInputElement | null;
  const label   = document.getElementById('progressModalLabel2');

  slider?.addEventListener('input', () => { label!.textContent = slider.value; });

  document.getElementById('progressModalSave')?.addEventListener('click', async () => {
    if (!progressCourseId) return;
    const spinner = document.getElementById('progressModalSpinner');
    spinner?.classList.remove('d-none');
    try {
      await apiPatch(`/courses/${progressCourseId}/enrollment`, { progress: Number(slider!.value) });
      progressModal?.hide();
      // Reload the courses list to reflect new status
      const updated = await fetchMyCourses();
      allCourses = updated;
      applyFilter();
    } catch (err: any) {
      toast.error('Lỗi cập nhật tiến độ: ' + (err?.message || 'Vui lòng thử lại'));
    } finally {
      spinner?.classList.add('d-none');
    }
  });
}

function openProgressModal(courseId: number | string, courseName: string, currentProgress: number): void {
  progressCourseId = courseId;
  const slider = document.getElementById('progressModalSlider') as HTMLInputElement | null;
  const label  = document.getElementById('progressModalLabel2');
  const name   = document.getElementById('progressModalCourseName');
  if (slider) slider.value = String(currentProgress ?? 0);
  if (label)  label.textContent = String(currentProgress ?? 0);
  if (name)   name.textContent  = courseName;
  progressModal?.show();
}

// ── Main ────────────────────────────────────────────────────────────────────
let allCourses: EnrolledCourseResponse[] = [];

function applyFilter(): void {
  const grid      = document.getElementById('myCoursesGrid')!;
  const statusVal = (document.querySelector('input[name="statusFilter"]:checked') as HTMLInputElement | null)?.value || 'all';
  const searchVal = (document.getElementById('courseSearch') as HTMLInputElement | null)?.value.toLowerCase() || '';
  const sortVal   = (document.getElementById('sortSelect') as HTMLSelectElement | null)?.value || 'title';

  let filtered = [...allCourses];
  if (searchVal) filtered = filtered.filter((c) => c.name.toLowerCase().includes(searchVal));
  if (statusVal !== 'all' && statusVal !== 'starred') {
    filtered = filtered.filter((c) => c.status === statusVal);
  }
  if (statusVal === 'starred') filtered = filtered.filter((c) => c.starred);
  if (sortVal === 'title')      filtered.sort((a, b) => a.name.localeCompare(b.name));
  if (sortVal === 'lastaccess') filtered.sort((a, b) => new Date(b.lastAccessed || 0).getTime() - new Date(a.lastAccessed || 0).getTime());

  const emptyEl = document.getElementById('myCoursesEmpty');
  if (filtered.length === 0) {
    grid.innerHTML = '';
    emptyEl?.classList.remove('d-none');
  } else {
    emptyEl?.classList.add('d-none');
    const viewMode = (document as any)._viewMode || 'card';
    const renderer = viewMode === 'list' ? renderCourseRow : viewMode === 'summary' ? renderCourseSummary : renderCourseCard;
    grid.innerHTML = filtered.map(renderer).join('');
  }
}

async function initMyCourses(): Promise<void> {
  const grid = document.getElementById('myCoursesGrid');
  if (!grid) return;
  if (!isLoggedIn()) {
    window.location.replace(`/pages/login/?redirect=${encodeURIComponent(window.location.pathname)}`);
    return;
  }
  setUserInitials();
  initProgressModal();

  // Loading skeleton
  grid.innerHTML = '<div class="col-12 text-center py-5"><span class="spinner-border"></span></div>';
  allCourses = await fetchMyCourses();
  (document as any)._viewMode = 'card';

  document.getElementById('courseSearch')?.addEventListener('input', applyFilter);
  document.getElementById('sortSelect')?.addEventListener('change', applyFilter);
  document.querySelectorAll('input[name="statusFilter"]').forEach((r) => r.addEventListener('change', applyFilter));

  const setView = (mode: string) => {
    (document as any)._viewMode = mode;
    ['viewCard', 'viewList', 'viewSummary'].forEach((id) => {
      document.getElementById(id)?.classList.toggle('active', id === `view${mode.charAt(0).toUpperCase() + mode.slice(1)}`);
    });
    applyFilter();
  };

  document.getElementById('viewCard')?.addEventListener('click',    () => setView('card'));
  document.getElementById('viewList')?.addEventListener('click',    () => setView('list'));
  document.getElementById('viewSummary')?.addEventListener('click', () => setView('summary'));

  // Delegated click for progress-update buttons
  grid.addEventListener('click', (e: Event) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>('.update-progress-btn');
    if (btn) {
      openProgressModal(
        btn.dataset.courseId!,
        btn.dataset.courseName!,
        Number(btn.dataset.progress),
      );
    }
  });

  applyFilter();
}

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initMyCourses();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
