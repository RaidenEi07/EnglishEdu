/**
 * Shared page footer — injects the standard 3-column footer into #page-footer.
 * Call injectFooter() BEFORE initNavbar() so footerLogoutBtn is available for binding.
 */
import { isLoggedIn } from './auth.ts';
import type { StoredUser } from './types.ts';

export function injectFooter(): void {
  const el = document.getElementById('page-footer');
  if (!el) return;
  el.className = 'bg-dark text-light';

  // Build the center column based on auth state
  let authSection: string;
  if (isLoggedIn()) {
    authSection = `<span class="small">Bạn đang đăng nhập với tên
      <a href="/pages/profile/" class="text-info text-decoration-none" id="footerUserLink">Học viên</a>
      (<a href="#" id="footerLogoutBtn" class="text-info text-decoration-none" data-i18n="nav.logout">Thoát</a>)</span>`;
  } else {
    authSection = `<span class="small" data-i18n="auth.not_logged_in">Bạn chưa đăng nhập.</span>
      <span class="small">(<a href="/pages/login/" class="text-info text-decoration-none" data-i18n="nav.login">Đăng nhập</a>)</span>`;
  }

  el.innerHTML = `
    <div class="container-fluid py-4">
      <div class="row">
        <div class="col-md-4">
          <h3 class="footer-title h6" data-i18n="footer.contact">Contact us</h3>
          <ul class="list-unstyled small">
            <li class="mb-1"><a href="https://pbs.sunshineschool.io.vn/pbschool" target="_blank" rel="noopener" class="text-light text-decoration-none"><i class="fa fa-globe me-2"></i>Website</a></li>
            <li class="mb-1"><a href="tel:0979289466" class="text-light text-decoration-none"><i class="fa fa-phone me-2"></i>0979 289 466 / 0985 289 466</a></li>
          </ul>
          <h3 class="footer-title h6 mt-3" data-i18n="footer.address">Địa chỉ</h3>
          <ul class="list-unstyled small">
            <li class="mb-1"><i class="fa fa-map-marker-alt me-1"></i> Trụ sở chính: K7/210 đường Hoàng Quốc Việt, Q.Cầu Giấy, TP.Hà Nội</li>
            <li class="mb-1"><i class="fa fa-map-marker-alt me-1"></i> Cơ sở 1: 33 Nguyễn Thị Minh Khai, P.Xương Giang, TP.Bắc Giang</li>
            <li class="mb-1"><i class="fa fa-map-marker-alt me-1"></i> Cơ sở 2: Lô 09 đường Pháp Loa, TDP 4, TT.Nham Biền, H.Yên Dũng, T.Bắc Giang</li>
          </ul>
        </div>
        <div class="col-md-4 text-center">
          <div class="mt-3">${authSection}</div>
          <div class="mt-2"><a href="#" class="text-info small text-decoration-none" data-i18n="footer.data_privacy">Data retention summary</a></div>
        </div>
        <div class="col-md-4 text-md-end">
          <h3 class="footer-title h6" data-i18n="footer.working_hours">Thời gian làm việc</h3>
          <ul class="list-unstyled small">
            <li>Thứ 3 - Thứ 6: 8AM đến 9.30PM</li>
            <li>Thứ 7, Chủ nhật: 7.30AM đến 7PM</li>
            <li>Thứ 2: Nghỉ</li>
          </ul>
          <div class="d-flex flex-column align-items-md-end gap-2 mt-3">
            <a href="https://play.google.com/store/apps/details?id=com.moodle.moodlemobile" target="_blank" rel="noopener">
              <img src="/images/store_google.svg" alt="Play Store" height="40">
            </a>
            <a href="https://apps.apple.com/app/id633359593" target="_blank" rel="noopener">
              <img src="/images/store_apple.svg" alt="App Store" height="40">
            </a>
          </div>
        </div>
      </div>
    </div>
    <div class="container-fluid py-2 border-top border-secondary">
      <div class="text-center small" data-i18n="footer.copyright">Copyright ©2024 <a href="/index.html" class="text-info text-decoration-none">Sunshine</a>. Powered by <a href="https://videaedtech.com/" class="text-info text-decoration-none" target="_blank" rel="noopener">Videa Edtech</a>. All Rights Reserved</div>
    </div>`;

  // Set display name from localStorage (may be overridden by page-specific logic with fresh API data)
  const userLink = document.getElementById('footerUserLink');
  if (userLink && isLoggedIn()) {
    try {
      const user: StoredUser = JSON.parse(localStorage.getItem('sso_user') || '{}');
      const displayName = user.fullName
        || [user.firstName, user.lastName].filter(Boolean).join(' ')
        || user.username || '';
      if (displayName) userLink.textContent = displayName;
    } catch { /* noop */ }
  }
}
