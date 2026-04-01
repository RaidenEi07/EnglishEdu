/**
 * Navbar - Shared auth UI logic for all pages
 * Handles: user menu visibility, user initials, logout, auth-required guards,
 *          role-based menu items, notification badge & dropdown
 */
import { isLoggedIn, logout } from './auth.ts';
import { apiGet, apiPut } from './api.ts';
import type { NotificationResponse, StoredUser } from './types.ts';

/* ── Helpers ────────────────────────────────────────────────── */
function escapeHtml(str: string): string {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function timeAgo(isoString: string | null): string {
  if (!isoString) return '';
  const diff = Date.now() - new Date(isoString).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1)  return 'Vừa xong';
  if (mins < 60) return `${mins} phút trước`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)  return `${hrs} giờ trước`;
  return `${Math.floor(hrs / 24)} ngày trước`;
}

/* ── Notifications ──────────────────────────────────────────── */
async function loadNotifications(): Promise<void> {
  try {
    const [notifs, count] = await Promise.all([
      apiGet<NotificationResponse[]>('/notifications'),
      apiGet<number>('/notifications/unread/count'),
    ]);

    // Badge
    const badge = document.getElementById('notifBadge');
    if (badge && count > 0) {
      badge.textContent = count > 9 ? '9+' : String(count);
      badge.classList.remove('d-none');
    }

    // List
    const list = document.getElementById('notificationList');
    if (list && notifs.length > 0) {
      list.innerHTML = notifs.slice(0, 10).map((n) => `
        <a href="${n.link || '#'}" class="dropdown-item py-2 border-bottom ${n.read ? '' : 'fw-semibold bg-light'}">
          <div class="small text-wrap lh-sm">${escapeHtml(n.message)}</div>
          <div class="text-muted" style="font-size:.7rem;margin-top:2px">${timeAgo(n.createdAt)}</div>
        </a>`).join('');
    }

    // Mark-all-read button
    const markAllBtn = document.getElementById('markAllReadBtn');
    markAllBtn?.addEventListener('click', async (e) => {
      e.preventDefault();
      try {
        await apiPut('/notifications/read-all', {});
        if (badge) badge.classList.add('d-none');
        document.querySelectorAll('#notificationList .fw-semibold')
          .forEach((el) => { el.classList.remove('fw-semibold', 'bg-light'); });
      } catch { /* noop */ }
    });
  } catch { /* noop */ }
}

/* ── Role menu injection ────────────────────────────────────── */
function injectRoleMenu(role: string): void {
  const path        = window.location.pathname;
  const dropdown    = document.getElementById('userMenu')?.querySelector('.dropdown-menu, ul.dropdown-menu');
  const primaryNavUl = document.querySelector<HTMLElement>('.primary-navigation ul');
  const sidebarGroup = document.querySelector<HTMLElement>('#sideDrawer .list-group');

  if (role === 'ADMIN') {
    const isManage = path.startsWith('/pages/manage');
    const isAdmin  = path.startsWith('/pages/admin');

    // "Quản lý" link
    if (primaryNavUl && !primaryNavUl.querySelector('a[href="/pages/manage/"]')) {
      primaryNavUl.insertAdjacentHTML('beforeend',
        `<li class="nav-item">
           <a class="nav-link${isManage ? ' active fw-bold' : ' fw-semibold'}" href="/pages/manage/" style="color:var(--sso-primary)">
             <i class="fa fa-shield-halved me-1"></i>Quản lý
           </a>
         </li>`);
    }

    // "Quản trị" link
    if (primaryNavUl && !primaryNavUl.querySelector('a[href="/pages/admin/"]')) {
      primaryNavUl.insertAdjacentHTML('beforeend',
        `<li class="nav-item">
           <a class="nav-link${isAdmin ? ' active fw-bold' : ' fw-semibold'}" href="/pages/admin/" style="color:var(--sso-primary)">
             <i class="fa fa-cogs me-1"></i>Quản trị
           </a>
         </li>`);
    }

    // Sidebar
    if (sidebarGroup) {
      if (!sidebarGroup.querySelector('a[href="/pages/admin/"]')) {
        sidebarGroup.insertAdjacentHTML('beforeend',
          `<a href="/pages/admin/" class="list-group-item list-group-item-action${isAdmin ? ' active' : ''}" style="color:var(--sso-primary)">
             <i class="fa fa-cogs me-2"></i>Quản trị
           </a>`);
      }
      if (!sidebarGroup.querySelector('a[href="/pages/manage/"]')) {
        sidebarGroup.insertAdjacentHTML('beforeend',
          `<a href="/pages/manage/" class="list-group-item list-group-item-action${isManage ? ' active' : ''}" style="color:var(--sso-primary)">
             <i class="fa fa-shield-halved me-2"></i>Quản lý
           </a>`);
      }
    }

    // Dropdown
    if (dropdown) {
      dropdown.insertAdjacentHTML('afterbegin',
        `<li><a class="dropdown-item fw-semibold" href="/pages/admin/" style="color:var(--sso-primary)">
           <i class="fa fa-cogs me-2"></i>Quản trị
         </a></li>
         <li><a class="dropdown-item fw-semibold" href="/pages/manage/" style="color:var(--sso-primary)">
           <i class="fa fa-shield-halved me-2"></i>Quản lý
         </a></li>
         <li><hr class="dropdown-divider"></li>`);
    }
  } else if (role === 'TEACHER') {
    const isTeacher = path.startsWith('/pages/teacher');

    // Primary nav
    if (primaryNavUl && !primaryNavUl.querySelector('a[href*="/pages/teacher/"]')) {
      primaryNavUl.insertAdjacentHTML('beforeend',
        `<li class="nav-item">
           <a class="nav-link${isTeacher ? ' active fw-bold' : ' fw-semibold'}" href="/pages/teacher/dashboard/" style="color:#198754">
             <i class="fa fa-chalkboard-teacher me-1"></i>Giáo viên
           </a>
         </li>`);
    }

    // Sidebar
    if (sidebarGroup && !sidebarGroup.querySelector('a[href*="/pages/teacher/"]')) {
      sidebarGroup.insertAdjacentHTML('beforeend',
        `<a href="/pages/teacher/dashboard/" class="list-group-item list-group-item-action${isTeacher ? ' active' : ''}" style="color:#198754">
           <i class="fa fa-chalkboard-teacher me-2"></i>Giáo viên
         </a>`);
    }

    // Dropdown
    if (dropdown) {
      dropdown.insertAdjacentHTML('afterbegin',
        `<li><a class="dropdown-item fw-semibold" href="/pages/teacher/dashboard/" style="color:#198754">
           <i class="fa fa-chalkboard-teacher me-2"></i>Giáo viên
         </a></li>
         <li><hr class="dropdown-divider"></li>`);
    }
  }
}

/* ── Main ───────────────────────────────────────────────────── */
export function initNavbar(): void {
  const userMenu    = document.getElementById('userMenu');
  const loginBtn    = document.getElementById('loginBtn');
  const logoutBtn   = document.getElementById('logoutBtn');
  const drawerLogin = document.getElementById('drawerLogin');

  if (isLoggedIn()) {
    userMenu?.classList.remove('d-none');
    loginBtn?.classList.add('d-none');
    drawerLogin?.classList.add('d-none');

    const userInitials = document.getElementById('userInitials');
    let role = 'STUDENT';
    if (userInitials) {
      try {
        const user: StoredUser = JSON.parse(localStorage.getItem('sso_user') || '{}');
        role = user.role || 'STUDENT';
        const name  = user.fullName || user.username || '';
        const parts = name.trim().split(/\s+/);
        userInitials.textContent = parts.length >= 2
          ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
          : (name.slice(0, 2).toUpperCase() || '?');
      } catch { userInitials.textContent = '?'; }
    }

    injectRoleMenu(role);
    loadNotifications();
  } else {
    userMenu?.classList.add('d-none');
    // Hide notifications/messages for guests
    document.getElementById('notificationMenu')?.classList.add('d-none');
    document.getElementById('messageMenu')?.classList.add('d-none');
  }

  // Auth-required link guard
  document.querySelectorAll('[data-auth-required]').forEach((link) => {
    link.addEventListener('click', (e) => {
      if (!isLoggedIn()) {
        e.preventDefault();
        const dest = link.getAttribute('href') || '/';
        window.location.href = `/pages/login/?redirect=${encodeURIComponent(dest)}`;
      }
    });
  });

  // Logout handlers (navbar + footer)
  [logoutBtn, document.getElementById('footerLogoutBtn')].forEach((btn) => {
    btn?.addEventListener('click', (e) => { e.preventDefault(); logout(); });
  });
}
