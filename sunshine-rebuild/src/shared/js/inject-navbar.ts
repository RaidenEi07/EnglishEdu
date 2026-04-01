/**
 * Shared navbar — injects the standard responsive navbar into <nav id="main-navbar">.
 * Call injectNavbar() BEFORE initNavbar() so all elements exist when initNavbar() runs.
 */

export function injectNavbar(): void {
  const el = document.getElementById('main-navbar');
  if (!el) return;

  const path = window.location.pathname;
  const isHome      = path === '/' || path === '/index.html';
  const isDashboard = path.startsWith('/pages/my/dashboard');
  const isMyCourses = path.startsWith('/pages/my/courses');

  el.className = 'navbar fixed-top navbar-expand-md navbar-light bg-white shadow';
  el.setAttribute('aria-label', 'Định hướng trang');

  el.innerHTML = `
    <div class="container-fluid">
      <button class="navbar-toggler border-0 d-block d-md-none" type="button"
              data-bs-toggle="offcanvas" data-bs-target="#sideDrawer" aria-controls="sideDrawer">
        <span class="navbar-toggler-icon"></span>
      </button>

      <a href="/index.html" class="navbar-brand d-none d-md-flex align-items-center m-0 me-4 p-0">
        <img src="/images/logo.jpg" class="logo me-1" alt="SSO" height="40">
      </a>

      <div class="primary-navigation">
        <ul class="nav navbar-nav">
          <li class="nav-item">
            <a class="nav-link${isHome ? ' active' : ''}" href="/index.html"${isHome ? ' aria-current="true"' : ''} data-i18n="nav.home">Trang chủ</a>
          </li>
          <li class="nav-item">
            <a class="nav-link${isDashboard ? ' active' : ''}" href="/pages/my/dashboard/"${isDashboard ? ' aria-current="true"' : ''} data-i18n="nav.dashboard" data-auth-required>Bảng Điều khiển</a>
          </li>
          <li class="nav-item">
            <a class="nav-link${isMyCourses ? ' active' : ''}" href="/pages/my/courses/"${isMyCourses ? ' aria-current="true"' : ''} data-i18n="nav.my_courses" data-auth-required>Các khoá học của tôi</a>
          </li>
        </ul>
      </div>

      <div class="navbar-nav ms-auto d-flex flex-row align-items-center gap-1">
        <!-- Notifications -->
        <div class="dropdown" id="notificationMenu">
          <a href="#" class="nav-link position-relative px-2" data-bs-toggle="dropdown" aria-label="Thông báo">
            <i class="fa-regular fa-bell"></i>
            <span class="badge bg-danger rounded-pill position-absolute top-0 start-100 translate-middle d-none" id="notifBadge" style="font-size:.6rem;"></span>
          </a>
          <div class="dropdown-menu dropdown-menu-end shadow-lg" style="min-width: 320px;">
            <div class="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">
              <h6 class="mb-0" data-i18n="dashboard.notifications">Các thông báo</h6>
              <a href="#" id="markAllReadBtn" class="small text-decoration-none" data-i18n="dashboard.mark_all_read">Đã đọc tất cả</a>
            </div>
            <div id="notificationList" class="overflow-auto" style="max-height: 300px;">
              <div class="text-center text-muted py-3 small" data-i18n="dashboard.no_notifications">Bạn không có thông báo nào</div>
            </div>
          </div>
        </div>

        <!-- Messages -->
        <div class="dropdown" id="messageMenu">
          <a href="#" class="nav-link position-relative px-2" data-bs-toggle="dropdown" aria-label="Tin nhắn">
            <i class="fa-regular fa-comment-dots"></i>
            <span class="badge bg-danger rounded-pill position-absolute top-0 start-100 translate-middle d-none" id="msgBadge" style="font-size:.6rem;"></span>
          </a>
          <div class="dropdown-menu dropdown-menu-end shadow-lg" style="min-width: 300px;">
            <div class="text-center text-muted py-3 small" data-i18n="dashboard.no_messages">Không có tin nhắn mới</div>
          </div>
        </div>

        <!-- Language Switcher -->
        <div class="dropdown">
          <a href="#" class="btn dropdown-toggle" id="langToggle" data-bs-toggle="dropdown" aria-label="Ngôn ngữ">
            <i class="fa fa-language me-1"></i>
            <span class="d-none d-sm-inline" data-i18n="nav.language">Vietnamese ‎(vi)‎</span>
          </a>
          <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="langToggle">
            <li><a class="dropdown-item" href="#" data-lang="en">English ‎(en)‎</a></li>
            <li><a class="dropdown-item" href="#" data-lang="vi">Vietnamese ‎(vi)‎</a></li>
          </ul>
        </div>

        <div class="vr d-none d-md-block"></div>

        <!-- Login button (shown when NOT logged in) -->
        <a href="/pages/login/" class="btn btn-link text-decoration-none" id="loginBtn" data-i18n="nav.login">Đăng nhập</a>

        <!-- User menu (shown when logged in, hidden by default) -->
        <div class="dropdown d-none" id="userMenu">
          <a href="#" class="btn dropdown-toggle d-flex align-items-center gap-1 px-2" id="userMenuToggle" data-bs-toggle="dropdown" aria-label="Tài khoản">
            <span class="user-initials rounded-circle bg-primary text-white d-flex align-items-center justify-content-center" id="userInitials" style="width:32px;height:32px;font-size:.8rem;font-weight:600;">?</span>
          </a>
          <ul class="dropdown-menu dropdown-menu-end shadow-lg">
            <li><a class="dropdown-item" href="/pages/profile/"><i class="fa fa-user me-2"></i><span data-i18n="nav.profile">Hồ sơ</span></a></li>
            <li><a class="dropdown-item" href="/pages/my/dashboard/"><i class="fa fa-tachometer-alt me-2"></i><span data-i18n="nav.dashboard">Bảng Điều khiển</span></a></li>
            <li><a class="dropdown-item" href="/pages/my/courses/"><i class="fa fa-graduation-cap me-2"></i><span data-i18n="nav.my_courses">Các khoá học của tôi</span></a></li>
            <li><hr class="dropdown-divider"></li>
            <li><a class="dropdown-item text-danger" href="#" id="logoutBtn"><i class="fa fa-sign-out-alt me-2"></i><span data-i18n="nav.logout">Đăng xuất</span></a></li>
          </ul>
        </div>
      </div>
    </div>`;

  // Inject offcanvas sidebar for mobile (only if not already in the DOM)
  if (!document.getElementById('sideDrawer')) {
    const drawer = document.createElement('div');
    drawer.className = 'offcanvas offcanvas-start';
    drawer.tabIndex = -1;
    drawer.id = 'sideDrawer';
    drawer.setAttribute('aria-labelledby', 'sideDrawerLabel');
    drawer.innerHTML = `
      <div class="offcanvas-header">
        <h5 class="offcanvas-title" id="sideDrawerLabel">
          <img src="/images/logo.jpg" alt="SSO" height="30">
        </h5>
        <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Đóng"></button>
      </div>
      <div class="offcanvas-body p-0">
        <div class="list-group list-group-flush">
          <a href="/index.html" class="list-group-item list-group-item-action${isHome ? ' active' : ''}"${isHome ? ' aria-current="true"' : ''} data-i18n="nav.home">
            <i class="fa fa-home me-2"></i>Trang chủ
          </a>
          <a href="/pages/my/dashboard/" class="list-group-item list-group-item-action${isDashboard ? ' active' : ''}"${isDashboard ? ' aria-current="true"' : ''} data-i18n="nav.dashboard" data-auth-required>
            <i class="fa fa-tachometer-alt me-2"></i>Bảng Điều khiển
          </a>
          <a href="/pages/my/courses/" class="list-group-item list-group-item-action${isMyCourses ? ' active' : ''}"${isMyCourses ? ' aria-current="true"' : ''} data-i18n="nav.my_courses" data-auth-required>
            <i class="fa fa-graduation-cap me-2"></i>Các khoá học của tôi
          </a>
          <a href="/pages/login/" class="list-group-item list-group-item-action" id="drawerLogin" data-i18n="nav.login">
            <i class="fa fa-sign-in-alt me-2"></i>Đăng nhập
          </a>
        </div>
      </div>`;
    el.insertAdjacentElement('afterend', drawer);
  }
}
