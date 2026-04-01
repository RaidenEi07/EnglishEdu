import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import '../teacher.css';
import 'bootstrap';

import { initI18n }   from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { apiGet }     from '../../../src/shared/js/api.ts';
import { toast }      from '../../../src/shared/js/toast.ts';
import { requireTeacher, formatDate } from '../teacher-utils.ts';
import type { CourseResponse, TeacherDashboardResponse, Page, MoodleLaunchResponse } from '../../../src/shared/js/types.ts';

// ── helpers ──────────────────────────────────────────────────────────────────
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function levelBadge(level: string | null | undefined): string {
  const map: Record<string, string> = { beginner: 'bg-success', intermediate: 'bg-warning text-dark', advanced: 'bg-danger' };
  return level
    ? `<span class="badge ${map[level] || 'bg-secondary'}">${escHtml(level)}</span>`
    : '<span class="text-muted">—</span>';
}

// ── stats ─────────────────────────────────────────────────────────────────────
async function loadStats(): Promise<void> {
  try {
    const data = await apiGet<TeacherDashboardResponse>('/teacher/dashboard');
    document.getElementById('statCourses')!.textContent  = String(data.totalCourses);
    document.getElementById('statStudents')!.textContent = String(data.totalStudents);
    document.getElementById('statPending')!.textContent  = String(data.pendingStudents);
    document.getElementById('statActive')!.textContent   = String(data.activeStudents);
  } catch (err) {
    console.error('Failed to load teacher dashboard stats', err);
    ['statCourses', 'statStudents', 'statPending', 'statActive'].forEach((id) => {
      document.getElementById(id)!.textContent = '—';
    });
  }
}

// ── courses ───────────────────────────────────────────────────────────────────
function renderCourseCard(c: CourseResponse): string {
  const img = c.imageUrl
    ? `<img src="${escHtml(c.imageUrl)}" class="card-img-top object-fit-cover" style="height:140px" alt="">`
    : `<div class="bg-success bg-opacity-10 d-flex align-items-center justify-content-center" style="height:140px">
         <i class="fa fa-book-open fa-3x text-success opacity-50"></i>
       </div>`;
  const studentsLink = `/pages/teacher/students/?courseId=${c.id}`;
  return `
<div class="col-sm-6 col-lg-4 col-xl-3">
  <div class="card h-100 shadow-sm">
    ${img}
    <div class="card-body d-flex flex-column gap-1">
      <h6 class="card-title mb-1 text-truncate" title="${escHtml(c.name)}">${escHtml(c.name)}</h6>
      <div class="d-flex gap-1 flex-wrap">
        ${c.category ? `<span class="badge bg-secondary">${escHtml(c.category)}</span>` : ''}
        ${levelBadge(c.level)}
        ${c.published ? '<span class="badge bg-success">Công khai</span>' : '<span class="badge bg-light text-dark border">Ẩn</span>'}
      </div>
      ${c.description ? `<p class="card-text text-muted small mt-1 mb-0" style="overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical">${escHtml(c.description)}</p>` : ''}
    </div>
    <div class="card-footer bg-transparent d-flex justify-content-between align-items-center gap-1 flex-wrap">
      <small class="text-muted">Mã: ${c.externalId ?? c.id}</small>
      <div class="d-flex gap-1 flex-wrap">
        <a href="/pages/learn/?id=${c.id}" class="btn btn-sm btn-outline-primary" title="Xem nội dung">
          <i class="fa fa-eye"></i>
        </a>
        <a href="${studentsLink}" class="btn btn-sm btn-outline-success">
          <i class="fa fa-users me-1"></i>Học viên
        </a>
        ${c.moodleCourseId ? `<button class="btn btn-sm btn-warning teacher-launch-btn" data-course-id="${c.id}" title="Soạn giáo án trên Moodle">
          <i class="fa fa-pen-to-square me-1"></i>Soạn giáo án
        </button>` : ''}
      </div>
    </div>
  </div>
</div>`;
}

let currentPage = 0;
let totalPages  = 0;

async function loadCourses(page: number = 0): Promise<void> {
  const container = document.getElementById('courseCards')!;
  container.innerHTML = '<div class="col-12 text-center py-4"><span class="spinner-border spinner-border-sm me-2"></span>Đang tải...</div>';
  try {
    const data = await apiGet<Page<CourseResponse>>(`/teacher/courses?page=${page}&size=12`);
    currentPage = data.number;
    totalPages  = data.totalPages;
    if (!data.content?.length) {
      container.innerHTML = '<div class="col-12 text-center text-muted py-4"><i class="fa fa-book-open fa-2x mb-2 d-block opacity-50"></i>Chưa có khóa học nào được phân công</div>';
      return;
    }
    container.innerHTML = data.content.map(renderCourseCard).join('');
    renderPagination(currentPage, totalPages);
  } catch (err) {
    console.error('Failed to load teacher courses', err);
    container.innerHTML = '<div class="col-12 text-center text-danger py-4"><i class="fa fa-triangle-exclamation me-2"></i>Lỗi khi tải danh sách khóa học</div>';
  }
}

function renderPagination(page: number, total: number): void {
  const existing = document.getElementById('coursePagination');
  if (existing) existing.remove();
  if (total <= 1) return;

  const nav = document.createElement('nav');
  nav.id = 'coursePagination';
  nav.className = 'mt-4 d-flex justify-content-center';
  const pages = [];
  for (let i = 0; i < total; i++) {
    pages.push(`<li class="page-item ${i === page ? 'active' : ''}">
      <button class="page-link" data-page="${i}">${i + 1}</button>
    </li>`);
  }
  nav.innerHTML = `<ul class="pagination">${pages.join('')}</ul>`;
  nav.addEventListener('click', (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>('[data-page]');
    if (btn) loadCourses(Number(btn.dataset.page));
  });
  document.getElementById('courseCards')!.after(nav);
}

// ── calendar (Moodle events) ──────────────────────────────────────────────────
function initCalendar(): void {
  const body       = document.getElementById('calendarBody');
  const monthLabel = document.getElementById('calMonthYear');
  const prevBtn    = document.getElementById('calPrev');
  const nextBtn    = document.getElementById('calNext');
  if (!body || !monthLabel) return;

  let current = new Date(); current.setDate(1);
  let eventDays = new Map<number, string[]>();

  const viMonths = ['tháng 1','tháng 2','tháng 3','tháng 4','tháng 5','tháng 6',
                    'tháng 7','tháng 8','tháng 9','tháng 10','tháng 11','tháng 12'];

  function render(): void {
    const year = current.getFullYear(), month = current.getMonth();
    monthLabel!.textContent = `${viMonths[month]} ${year}`;
    const firstDay    = new Date(year, month, 1).getDay();
    const startOffset = (firstDay + 6) % 7;
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today       = new Date();
    let html = '', dayCount = 1;
    const totalCells = Math.ceil((startOffset + daysInMonth) / 7) * 7;
    for (let i = 0; i < totalCells; i++) {
      if (i % 7 === 0) html += '<tr>';
      if (i < startOffset || dayCount > daysInMonth) {
        html += '<td class="text-muted opacity-25 small"></td>';
      } else {
        const isToday = dayCount === today.getDate() && month === today.getMonth() && year === today.getFullYear();
        const hasEvent = eventDays.has(dayCount);
        const cls   = isToday ? 'bg-primary text-white rounded-circle d-inline-block' : '';
        const style = isToday ? 'width:28px;height:28px;line-height:28px;text-align:center;' : '';
        let tooltip = '';
        if (hasEvent) {
          tooltip = eventDays.get(dayCount)!.join('\n');
        }
        const dot = hasEvent ? '<span class="schedule-dot"></span>' : '';
        html += `<td class="small position-relative${hasEvent ? ' has-schedule' : ''}" ${tooltip ? `title="${tooltip.replace(/"/g, '&quot;')}"` : ''}>`;
        html += `<span class="${cls}" style="${style}">${dayCount}</span>${dot}</td>`;
        dayCount++;
      }
      if (i % 7 === 6) html += '</tr>';
    }
    body!.innerHTML = html;
  }

  async function loadAndRender(): Promise<void> {
    const year = current.getFullYear(), month = current.getMonth();
    const timeStart = Math.floor(new Date(year, month, 1).getTime() / 1000);
    const timeEnd   = Math.floor(new Date(year, month + 1, 0, 23, 59, 59).getTime() / 1000);
    eventDays = new Map();
    try {
      const events = await apiGet<any[]>(`/moodle/calendar?timeStart=${timeStart}&timeEnd=${timeEnd}`);
      for (const ev of events) {
        const d = new Date((ev.timestart ?? 0) * 1000);
        if (d.getMonth() === month && d.getFullYear() === year) {
          const day = d.getDate();
          if (!eventDays.has(day)) eventDays.set(day, []);
          eventDays.get(day)!.push(ev.name ?? 'Event');
        }
      }
    } catch { /* Moodle unavailable */ }
    render();
  }

  prevBtn?.addEventListener('click', () => { current.setMonth(current.getMonth() - 1); loadAndRender(); });
  nextBtn?.addEventListener('click', () => { current.setMonth(current.getMonth() + 1); loadAndRender(); });
  loadAndRender();
}

// ── Teacher launch (SSO to Moodle course editing) ────────────────────────────
function initTeacherLaunch(): void {
  document.addEventListener('click', async (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>('.teacher-launch-btn');
    if (!btn) return;
    const courseId = btn.dataset.courseId;
    if (!courseId) return;

    btn.classList.add('disabled');
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang mở...';
    try {
      const res = await apiGet<MoodleLaunchResponse>(`/moodle/teacher-launch?courseId=${courseId}`);
      window.open(res.url, '_blank', 'noopener,noreferrer');
    } catch {
      toast.error('Không thể mở Moodle. Vui lòng thử lại sau.');
    } finally {
      btn.classList.remove('disabled');
      btn.innerHTML = '<i class="fa fa-pen-to-square me-1"></i>Soạn giáo án';
    }
  });
}

// ── init ──────────────────────────────────────────────────────────────────────
function initApp(): void {
  requireTeacher();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();
  Promise.all([loadStats(), loadCourses()]);
  initCalendar();
  initTeacherLaunch();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
