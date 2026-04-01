/**
 * Dashboard page entry point (/pages/my/dashboard/)
 */

/* --- Styles --- */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import './dashboard.css';

/* --- Bootstrap JS --- */
import 'bootstrap';

/* --- Shared modules --- */
import { initI18n } from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { isLoggedIn } from '../../../src/shared/js/auth.ts';
import { apiGet } from '../../../src/shared/js/api.ts';
import type { EnrolledCourseResponse } from '../../../src/shared/js/types.ts';

/* --- Section utilities shared with courses page --- */
import { fetchMyCourses, setUserInitials } from '../my-utils.ts';

async function fetchRecentItems(): Promise<EnrolledCourseResponse[]>   { try { return await apiGet<EnrolledCourseResponse[]>('/courses/recent'); } catch { return []; } }
async function fetchTimelineEvents(): Promise<any[]>{ try { return await apiGet<any[]>('/moodle/timeline'); } catch { return []; } }

/* ── Calendar ──────────────────────────────────────────────────── */
function initCalendar(): void {
  const body       = document.getElementById('calendarBody');
  const monthLabel = document.getElementById('calMonthYear');
  const prevBtn    = document.getElementById('calPrev');
  const nextBtn    = document.getElementById('calNext');
  if (!body || !monthLabel) return;

  let current = new Date(); current.setDate(1);
  let eventDays = new Map<number, string[]>(); // day -> event names

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

/* ── Timeline ──────────────────────────────────────────────── */
function initTimeline(): void {
  const content  = document.getElementById('timelineContent');
  const search   = document.getElementById('timelineSearch') as HTMLInputElement | null;
  const clearBtn = document.getElementById('timelineSearchClear');
  if (!content) return;

  search?.addEventListener('input', () => { clearBtn?.classList.toggle('d-none', !search.value); });
  clearBtn?.addEventListener('click', () => { if (search) search.value = ''; clearBtn?.classList.add('d-none'); });

  document.querySelectorAll<HTMLElement>('#blockTimeline [data-days]').forEach((item) => {
    item.addEventListener('click', (e: Event) => {
      e.preventDefault();
      const btn = document.getElementById('timelineDateFilter');
      if (btn) btn.querySelector('span')!.textContent = item.textContent;
      item.closest('ul')!.querySelectorAll('.dropdown-item').forEach((i) => i.classList.remove('active'));
      item.classList.add('active');
    });
  });

  document.querySelectorAll<HTMLElement>('#blockTimeline [data-sort]').forEach((item) => {
    item.addEventListener('click', (e: Event) => {
      e.preventDefault();
      const btn = document.getElementById('timelineSortBtn');
      if (btn) btn.querySelector('span')!.textContent = item.textContent;
      item.closest('ul')!.querySelectorAll('.dropdown-item').forEach((i) => i.classList.remove('active'));
      item.classList.add('active');
    });
  });
}

/* ── Recently accessed items ───────────────────────────────── */
async function loadRecentItems(): Promise<void> {
  const skeleton  = document.getElementById('recentItemsSkeleton');
  const contentEl = document.getElementById('recentItemsContent');
  const emptyEl   = document.getElementById('recentItemsEmpty');
  if (!skeleton) return;
  const items = await fetchRecentItems();
  skeleton.classList.add('d-none');
  if (items.length === 0) { emptyEl?.classList.remove('d-none'); return; }
  contentEl!.innerHTML = items.slice(0, 10).map((item) => `
    <a href="/pages/course/?id=${item.courseId}" class="d-flex align-items-center gap-2 mb-3 text-decoration-none text-body">
      <img src="${item.imageUrl || '/images/course-placeholder.svg'}" alt="" class="rounded" width="40" height="40" style="object-fit:cover;">
      <div class="min-width-0">
        <div class="small fw-medium text-truncate">${item.name}</div>
        <div class="text-muted" style="font-size:.75rem">${item.category || ''}</div>
      </div>
    </a>`).join('');
  contentEl!.classList.remove('d-none');
}

/* ── Timeline renderer ────────────────────────────────────────── */
function timeAgoShort(iso: string | null | undefined): string {
  if (!iso) return '';
  const d    = new Date(iso);
  const diff = (Date.now() - d.getTime()) / 1000;
  if (diff < 60)    return 'V\u1eeba xong';
  if (diff < 3600)  return `${Math.round(diff / 60)} ph\u00fat`;
  if (diff < 86400) return `${Math.round(diff / 3600)} gi\u1edd`;
  return d.toLocaleDateString('vi-VN');
}

const TYPE_META: Record<string, { icon: string; color: string }> = {
  assign:     { icon: 'fa-file-alt',        color: 'text-warning'   },
  quiz:       { icon: 'fa-question-circle',  color: 'text-info'      },
  lesson:     { icon: 'fa-book-open',        color: 'text-primary'   },
  url:        { icon: 'fa-link',             color: 'text-secondary' },
  forum:      { icon: 'fa-comments',         color: 'text-success'   },
};

function renderTimeline(items: any[]): void {
  const content = document.getElementById('timelineContent');
  if (!content) return;
  if (!items || items.length === 0) {
    content.innerHTML = '<p class="text-muted text-center small py-3">Ch\u01b0a c\u00f3 s\u1ef1 ki\u1ec7n n\u00e0o.</p>';
    return;
  }
  content.innerHTML = items.map((ev: any) => {
    const ts = ev.timesort ? new Date(ev.timesort * 1000).toISOString() : null;
    const modname = ev.modulename || '';
    const { icon, color } = TYPE_META[modname] || { icon: 'fa-calendar', color: 'text-secondary' };
    return `
      <div class="d-flex align-items-start gap-2 mb-3">
        <div class="flex-shrink-0 pt-1"><i class="fas ${icon} ${color}"></i></div>
        <div class="min-width-0 flex-grow-1">
          <span class="small fw-medium text-truncate d-block">${ev.name || ''}</span>
          <div class="text-muted" style="font-size:.75rem">${ev.course?.fullname || ''}</div>
        </div>
        <div class="flex-shrink-0 text-end">
          <div class="text-muted" style="font-size:.7rem">${timeAgoShort(ts)}</div>
        </div>
      </div>`;
  }).join('');
}

/* ── Student courses list ──────────────────────────────────── */
function renderStudentList(courses: EnrolledCourseResponse[]): void {
  const skeleton  = document.getElementById('studentListSkeleton');
  const contentEl = document.getElementById('studentListContent');
  const emptyEl   = document.getElementById('studentListEmpty');
  if (!skeleton) return;
  skeleton.classList.add('d-none');
  if (!courses.length) { emptyEl?.classList.remove('d-none'); return; }

  const STATUS_MAP: Record<string, [string, string]> = {
    pending:    ['bg-warning text-dark', 'Chờ duyệt'],
    active:     ['bg-primary',           'Đã duyệt'],
    inprogress: ['bg-info text-dark',    'Đang học'],
    completed:  ['bg-success',           'Hoàn thành'],
    revoked:    ['bg-danger',            'Thu hồi'],
  };

  contentEl!.innerHTML = courses.map((c) => {
    const [cls, label] = STATUS_MAP[c.status] || ['bg-secondary', c.status];
    const pct = c.progress ?? 0;
    return `
      <div class="d-flex align-items-center gap-2 mb-3">
        <img src="${c.imageUrl || '/images/course-placeholder.svg'}" alt="" class="rounded flex-shrink-0"
             width="32" height="32" style="object-fit:cover;">
        <div class="min-width-0 flex-fill">
          <a href="/pages/course/?id=${c.courseId}" class="small fw-medium text-decoration-none text-body text-truncate d-block">${c.name}</a>
          <div class="d-flex align-items-center gap-1 mt-1">
            <span class="badge ${cls}" style="font-size:.65rem">${label}</span>
            <div class="progress flex-fill" style="height:4px;">
              <div class="progress-bar" role="progressbar" style="width:${pct}%"></div>
            </div>
            <span class="text-muted" style="font-size:.7rem;white-space:nowrap">${pct}%</span>
          </div>
        </div>
      </div>`;
  }).join('');
  contentEl!.classList.remove('d-none');
}

/* ── Progress stats ────────────────────────────────────────── */
function updateStats(courses: EnrolledCourseResponse[]): void {
  const pending    = courses.filter((c) => c.status === 'pending').length;
  const inProgress = courses.filter((c) => c.status === 'inprogress').length;
  const done       = courses.filter((c) => c.status === 'completed').length;
  const active     = courses.filter((c) => ['active', 'inprogress', 'completed'].includes(c.status)).length;
  const total      = courses.length;
  const pct        = total > 0 ? Math.round((done / total) * 100) : 0;
  const el = (id: string) => document.getElementById(id);
  if (el('statEnrolled'))   el('statEnrolled')!.textContent   = String(total);
  if (el('statInProgress')) el('statInProgress')!.textContent = String(inProgress);
  if (el('statDone'))       el('statDone')!.textContent       = String(done);
  if (el('progressBar'))    (el('progressBar') as HTMLElement).style.width    = `${pct}%`;
  if (el('progressLabel'))  el('progressLabel')!.textContent  = `${done}/${total}`;
  // Show pending badge near stat if there are pending enrollments
  const enrolled = el('statEnrolled');
  if (enrolled && pending > 0) {
    const wrap = enrolled.closest('.p-2.rounded');
    if (wrap && !wrap.querySelector('.pending-badge')) {
      const badge = document.createElement('div');
      badge.className = 'pending-badge small text-warning fw-semibold';
      badge.textContent = `${pending} chờ duyệt`;
      wrap.appendChild(badge);
    }
  }
}

async function initDashboard(): Promise<void> {
  if (!document.getElementById('dashboardMain')) return;
  if (!isLoggedIn()) {
    window.location.replace(`/pages/login/?redirect=${encodeURIComponent(window.location.pathname)}`);
    return;
  }

  // Show role-appropriate section title
  try {
    const user = JSON.parse(localStorage.getItem('sso_user') || '{}');
    if (user?.role === 'STUDENT') {
      const titleEl = document.querySelector<HTMLElement>('#blockStudentList .card-title');
      if (titleEl) titleEl.textContent = 'Danh sách khóa học';
    }
  } catch { /* noop */ }

  setUserInitials();
  initCalendar();
  initTimeline();
  const [courses, , timelineItems] = await Promise.all([
    fetchMyCourses(),
    loadRecentItems(),
    fetchTimelineEvents(),
  ]);
  updateStats(courses);
  renderStudentList(courses);
  renderTimeline(timelineItems);
}

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initDashboard();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
