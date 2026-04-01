/**
 * Course Detail page
 * Shows full course info and handles enrollment
 */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import 'bootstrap';

import { initI18n } from '../../src/shared/js/i18n.ts';
import { initNavbar } from '../../src/shared/js/navbar.ts';
import { injectFooter } from '../../src/shared/js/footer.ts';
import { injectNavbar } from '../../src/shared/js/inject-navbar.ts';
import { isLoggedIn } from '../../src/shared/js/auth.ts';
import { apiGet, apiPost } from '../../src/shared/js/api.ts';
import { toast } from '../../src/shared/js/toast.ts';
import type { CourseResponse, EnrolledCourseResponse, StoredUser, MoodleLaunchResponse } from '../../src/shared/js/types.ts';

let isAssigned = false;

function show(id: string): void { document.getElementById(id)?.classList.remove('d-none'); }
function hide(id: string): void { document.getElementById(id)?.classList.add('d-none'); }
function setText(id: string, val: string): void { const el = document.getElementById(id); if (el) el.textContent = val; }
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

async function checkAssignment(courseId: number): Promise<boolean> {
  if (!isLoggedIn()) return false;
  try {
    const assigned = await apiGet<CourseResponse[]>('/courses/assigned');
    return assigned.some((c) => c.id === courseId);
  } catch {
    return false;
  }
}

async function loadEnrollmentStatus(courseId: number): Promise<string | null> {
  if (!isLoggedIn()) return null;
  try {
    const list = await apiGet<EnrolledCourseResponse[]>('/courses/enrolled');
    const entry = list.find((e) => Number(e.courseId) === courseId);
    return entry?.status ?? null;
  } catch {
    return null;
  }
}

function renderEnrollmentState(status: string | null): void {
  hide('enrollNotLogin');
  hide('enrollNotStudent');
  hide('enrollNotAssigned');
  hide('enrollPending');
  hide('enrollActive');
  hide('enrollCompleted');
  hide('enrollAction');
  hide('enrollTeacher');

  if (!isLoggedIn()) {
    show('enrollNotLogin');
    return;
  }

  const user: StoredUser | null = (() => {
    try { return JSON.parse(localStorage.getItem('sso_user') || 'null'); } catch { return null; }
  })();

  const isStudentOrAdmin = user && !user.guest && (user.role === 'STUDENT' || user.role === 'ADMIN');
  if (!isStudentOrAdmin) {
    // TEACHER role: show dedicated teacher panel with "Soạn giáo án chi tiết"
    if (user && user.role === 'TEACHER') {
      show('enrollTeacher');
      return;
    }
    show('enrollNotStudent');
    return;
  }

  if (status === 'pending')                       { show('enrollPending');   return; }
  if (status === 'active' || status === 'inprogress') { show('enrollActive');    return; }
  if (status === 'completed')                     { show('enrollCompleted'); return; }

  // Not enrolled or revoked – admins bypass enrollment entirely
  if (user.role === 'ADMIN') {
    show('enrollActive'); // Admin enters any course directly
    return;
  }

  if (!isAssigned) {
    show('enrollNotAssigned');
    return;
  }

  show('enrollAction');
}

/* ── Course modules rendering (from Moodle) ──────────────────── */
async function loadCourseModules(courseId: number): Promise<void> {
  const loadingEl = document.getElementById('courseModulesLoading');
  const emptyEl = document.getElementById('courseModulesEmpty');
  const accEl = document.getElementById('courseModulesAcc');
  const summaryEl = document.getElementById('courseModulesSummary');

  try {
    const sections = await apiGet<any[]>(`/moodle/course-contents?courseId=${courseId}`);
    loadingEl?.classList.add('d-none');

    if (!sections || sections.length === 0) {
      emptyEl?.classList.remove('d-none');
      return;
    }

    // Filter out empty sections
    const nonEmpty = sections.filter((s: any) => s.modules && s.modules.length > 0);
    if (nonEmpty.length === 0) {
      emptyEl?.classList.remove('d-none');
      return;
    }

    const totalModules = nonEmpty.reduce((sum: number, s: any) => sum + s.modules.length, 0);

    const typeIcon = (modname: string) => {
      const map: Record<string, string> = {
        resource: 'fa-file-pdf text-warning',
        url: 'fa-link text-secondary',
        assign: 'fa-file-alt text-primary',
        quiz: 'fa-question-circle text-info',
        forum: 'fa-comments text-success',
        page: 'fa-file-lines text-primary',
        label: 'fa-tag text-muted',
      };
      return map[modname] || 'fa-circle text-muted';
    };

    if (accEl) {
      accEl.innerHTML = nonEmpty.map((sec: any, i: number) => `
        <div class="accordion-item">
          <h2 class="accordion-header">
            <button class="accordion-button ${i > 0 ? 'collapsed' : ''} py-2" type="button"
                    data-bs-toggle="collapse" data-bs-target="#detMod${i}">
              <span class="fw-medium">${escHtml(sec.name)}</span>
              <span class="badge bg-secondary bg-opacity-25 text-dark ms-auto me-2">${sec.modules.length} mục</span>
            </button>
          </h2>
          <div id="detMod${i}" class="accordion-collapse collapse ${i === 0 ? 'show' : ''}">
            <ul class="list-group list-group-flush">
              ${sec.modules.map((m: any) => `
                <li class="list-group-item py-2 d-flex align-items-center">
                  <i class="fa ${typeIcon(m.modname)} me-2"></i>
                  <span class="small">${escHtml(m.name)}</span>
                </li>
              `).join('')}
            </ul>
          </div>
        </div>
      `).join('');
    }

    if (summaryEl) {
      summaryEl.textContent = `${nonEmpty.length} phần · ${totalModules} mục`;
      summaryEl.classList.remove('d-none');
    }
  } catch {
    loadingEl?.classList.add('d-none');
    emptyEl?.classList.remove('d-none');
  }
}

async function initCoursePage(): Promise<void> {
  const params   = new URLSearchParams(window.location.search);
  const courseId = params.get('id');

  if (!courseId || isNaN(Number(courseId))) {
    hide('courseLoading');
    show('courseError');
    setText('courseErrorMsg', 'Không tìm thấy ID khóa học trong URL.');
    return;
  }

  // Set login redirect for login button
  const loginBtn = document.getElementById('enrollLoginBtn') as HTMLAnchorElement | null;
  if (loginBtn) loginBtn.href = `/pages/login/?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;

  let course: CourseResponse;
  try {
    course = await apiGet<CourseResponse>(`/courses/${courseId}`);
  } catch {
    hide('courseLoading');
    show('courseError');
    setText('courseErrorMsg', 'Không tìm thấy khóa học hoặc đã xảy ra lỗi kết nối.');
    return;
  }

  // Render course info
  document.title = `${course.name} | SSO`;
  setText('breadcrumbName', course.name);
  setText('courseName',     course.name);
  setText('courseCategory', course.category ?? '');
  setText('courseLevel',    course.level ?? '');
  setText('courseTeacher',  course.teacherName ?? 'Chưa có giáo viên');
  setText('courseDescription', course.description || 'Chưa có mô tả cho khóa học này.');

  const img = document.getElementById('courseImage') as HTMLImageElement | null;
  if (img) img.src = course.imageUrl || '/images/course-placeholder.svg';

  hide('courseLoading');
  show('courseDetail');

  // Load course modules/lessons structure
  loadCourseModules(Number(courseId));

  // Set learn page links
  const learnUrl = `/pages/learn/?id=${courseId}`;
  const goLearnBtn = document.getElementById('goLearnBtn') as HTMLAnchorElement | null;
  const goReviewBtn = document.getElementById('goReviewBtn') as HTMLAnchorElement | null;
  if (goLearnBtn) goLearnBtn.href = learnUrl;
  if (goReviewBtn) goReviewBtn.href = learnUrl;

  // Check assignment + enrollment status, then render action
  isAssigned = await checkAssignment(Number(courseId));
  const status = await loadEnrollmentStatus(Number(courseId));
  renderEnrollmentState(status);

  // Show Moodle launch button if course is linked to Moodle
  const storedUser: StoredUser | null = (() => {
    try { return JSON.parse(localStorage.getItem('sso_user') || 'null'); } catch { return null; }
  })();
  const isAdmin = storedUser?.role === 'ADMIN';
  const isTeacher = storedUser?.role === 'TEACHER';
  if (course.moodleCourseId && (isAdmin || isTeacher || status === 'active' || status === 'inprogress' || status === 'completed')) {
    const launchBtn = document.getElementById('moodleLaunchBtn');
    const launchBtnCompleted = document.getElementById('moodleLaunchBtnCompleted');
    const btn = status === 'completed' ? launchBtnCompleted : launchBtn;
    if (btn) {
      btn.classList.remove('d-none');
      btn.addEventListener('click', async () => {
        try {
          const res = await apiGet<MoodleLaunchResponse>(`/moodle/launch?courseId=${courseId}`);
          window.open(res.url, '_blank', 'noopener,noreferrer');
        } catch {
          toast.error('Không thể mở Moodle. Vui lòng thử lại sau.');
        }
      });
    }
  }

  // Teacher "Soạn giáo án chi tiết" button – SSO to Moodle course editing
  if (course.moodleCourseId && (isTeacher || isAdmin)) {
    // Set learn link for teacher view
    const goLearnBtnTeacher = document.getElementById('goLearnBtnTeacher') as HTMLAnchorElement | null;
    if (goLearnBtnTeacher) goLearnBtnTeacher.href = learnUrl;

    const teacherLaunchBtn = document.getElementById('teacherLaunchBtn') as HTMLButtonElement | null;
    teacherLaunchBtn?.addEventListener('click', async () => {
      if (teacherLaunchBtn) {
        teacherLaunchBtn.disabled = true;
        teacherLaunchBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang mở Moodle...';
      }
      try {
        const res = await apiGet<MoodleLaunchResponse>(`/moodle/teacher-launch?courseId=${courseId}`);
        window.open(res.url, '_blank', 'noopener,noreferrer');
      } catch {
        toast.error('Không thể mở Moodle. Vui lòng thử lại sau.');
      } finally {
        if (teacherLaunchBtn) {
          teacherLaunchBtn.disabled = false;
          teacherLaunchBtn.innerHTML = '<i class="fa fa-pen-to-square me-2"></i>Soạn giáo án chi tiết';
        }
      }
    });
  }

  // Enroll button handler
  const enrollBtn = document.getElementById('enrollBtn') as HTMLButtonElement | null;
  enrollBtn?.addEventListener('click', async () => {
    const errEl = document.getElementById('enrollError');
    if (errEl) errEl.classList.add('d-none');
    enrollBtn.disabled = true;
    enrollBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang gửi yêu cầu...';
    try {
      await apiPost(`/courses/${courseId}/enroll`, {});
      toast.success('Đã gửi yêu cầu đăng ký! Vui lòng chờ admin xét duyệt.');
      renderEnrollmentState('pending');
    } catch (err: any) {
      if (errEl) {
        errEl.textContent = err.message || 'Không thể đăng ký. Vui lòng thử lại.';
        errEl.classList.remove('d-none');
      }
      enrollBtn.disabled = false;
      enrollBtn.innerHTML = '<i class="fa fa-user-plus me-2"></i>Đăng ký khóa học';
    }
  });
}

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initCoursePage();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
