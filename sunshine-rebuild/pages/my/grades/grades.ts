/**
 * Grades page – shows Moodle grades for enrolled courses
 * URL: /pages/my/grades/
 */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import 'bootstrap';

import { initI18n } from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { isLoggedIn } from '../../../src/shared/js/auth.ts';
import { apiGet } from '../../../src/shared/js/api.ts';
import type { EnrolledCourseResponse } from '../../../src/shared/js/types.ts';

function show(id: string) { document.getElementById(id)?.classList.remove('d-none'); }
function hide(id: string) { document.getElementById(id)?.classList.add('d-none'); }
function setText(id: string, v: string) { const el = document.getElementById(id); if (el) el.textContent = v; }
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

async function loadGrades(courseId: number): Promise<void> {
  hide('gradeTableContent');
  hide('gradeTableError');
  show('gradeTableLoading');
  show('gradeTableContainer');

  try {
    const res = await apiGet<any>(`/moodle/grades?courseId=${courseId}`);

    // Moodle gradereport_user_get_grade_items returns { usergrades: [{ gradeitems: [...] }] }
    const gradeItems: any[] = res?.usergrades?.[0]?.gradeitems ?? [];

    if (gradeItems.length === 0) {
      hide('gradeTableLoading');
      show('gradeTableError');
      setText('gradeTableErrorMsg', 'Chưa có điểm cho khóa học này.');
      return;
    }

    const tbody = document.getElementById('gradeTableBody')!;
    tbody.innerHTML = gradeItems.map((item: any) => {
      const name = escHtml(item.itemname || item.itemtype || '—');
      const grade = item.graderaw != null ? Number(item.graderaw).toFixed(1) : (item.gradeformatted || '—');
      const max = item.grademax != null ? Number(item.grademax).toFixed(0) : '';
      const pct = item.percentageformatted || (item.graderaw != null && item.grademax ? `${Math.round((item.graderaw / item.grademax) * 100)}%` : '');
      const feedback = escHtml(item.feedback || '');
      const isCategory = item.itemtype === 'category' || item.itemtype === 'course';
      const rowClass = isCategory ? 'table-light fw-medium' : '';
      return `<tr class="${rowClass}">
        <td>${isCategory ? `<strong>${name}</strong>` : name}</td>
        <td class="text-center">${grade}${max ? ` / ${max}` : ''}</td>
        <td class="text-center">${pct}</td>
        <td class="text-end small text-muted">${feedback}</td>
      </tr>`;
    }).join('');

    hide('gradeTableLoading');
    show('gradeTableContent');
  } catch {
    hide('gradeTableLoading');
    show('gradeTableError');
    setText('gradeTableErrorMsg', 'Không thể tải điểm từ Moodle. Vui lòng thử lại sau.');
  }
}

async function initGradesPage(): Promise<void> {
  if (!isLoggedIn()) {
    window.location.replace(`/pages/login/?redirect=${encodeURIComponent(window.location.pathname)}`);
    return;
  }

  try {
    const courses = await apiGet<EnrolledCourseResponse[]>('/courses/enrolled');
    hide('gradesLoading');

    const active = courses.filter(c => ['active', 'inprogress', 'completed'].includes(c.status));
    if (active.length === 0) {
      show('gradesEmpty');
      return;
    }

    show('gradesContent');
    const select = document.getElementById('courseSelect') as HTMLSelectElement;
    for (const c of active) {
      const opt = document.createElement('option');
      opt.value = String(c.courseId);
      opt.textContent = c.name;
      select.appendChild(opt);
    }

    select.addEventListener('change', () => {
      const val = select.value;
      if (!val) {
        hide('gradeTableContainer');
        return;
      }
      const course = active.find(c => String(c.courseId) === val);
      if (course) setText('gradeCourseName', course.name);
      loadGrades(Number(val));
    });

    // Auto-select from URL param
    const params = new URLSearchParams(window.location.search);
    const courseIdParam = params.get('courseId');
    if (courseIdParam && active.some(c => String(c.courseId) === courseIdParam)) {
      select.value = courseIdParam;
      const course = active.find(c => String(c.courseId) === courseIdParam);
      if (course) setText('gradeCourseName', course.name);
      loadGrades(Number(courseIdParam));
    }
  } catch {
    hide('gradesLoading');
    show('gradesError');
    setText('gradesErrorMsg', 'Không thể tải danh sách khóa học.');
  }
}

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initGradesPage();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
