/**
 * Login page entry point
 */

/* --- Styles --- */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import './login.css';

/* --- Bootstrap JS --- */
import 'bootstrap';

/* --- Shared modules --- */
import { initI18n } from '../../src/shared/js/i18n.ts';
import { login, guestLogin, register } from '../../src/shared/js/auth.ts';

function safeRedirect(url: string | null): string {
  if (!url) return '/';
  try {
    const parsed = new URL(url, window.location.origin);
    return parsed.origin === window.location.origin ? parsed.pathname + parsed.search : '/';
  } catch {
    return '/';
  }
}

function showError(el: HTMLElement | null, msg: string) {
  if (!el) return;
  el.textContent = msg;
  el.classList.remove('d-none');
}
function hideError(el: HTMLElement | null) {
  if (!el) return;
  el.textContent = '';
  el.classList.add('d-none');
}

function initLoginForm(): void {
  const form    = document.getElementById('loginForm');
  const errorEl = document.getElementById('loginError');
  if (!form) return;

  form.addEventListener('submit', async (e: Event) => {
    e.preventDefault();
    const username = (document.getElementById('username') as HTMLInputElement).value.trim();
    const password = (document.getElementById('password') as HTMLInputElement).value;
    if (!username || !password) {
      showError(errorEl, 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    const submitBtn = form.querySelector<HTMLButtonElement>('[type="submit"]');
    const origText  = submitBtn?.textContent ?? '';
    if (submitBtn) { submitBtn.disabled = true; submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang đăng nhập...'; }
    hideError(errorEl);
    try {
      await login(username, password);
      const redirect = new URLSearchParams(window.location.search).get('redirect');
      window.location.href = safeRedirect(redirect);
    } catch (err: any) {
      showError(errorEl, err.message);
      if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = origText; }
    }
  });

  const guestBtn = document.getElementById('guestLoginBtn');
  guestBtn?.addEventListener('click', async () => {
    try {
      await guestLogin();
      const redirect = new URLSearchParams(window.location.search).get('redirect');
      window.location.href = safeRedirect(redirect);
    } catch { /* Guest login not available */ }
  });
}

function initRegisterForm(): void {
  const form      = document.getElementById('registerForm');
  const errorEl   = document.getElementById('registerError');
  const successEl = document.getElementById('registerSuccess');
  if (!form) return;

  form.addEventListener('submit', async (e: Event) => {
    e.preventDefault();
    hideError(errorEl);
    if (successEl) { successEl.textContent = ''; successEl.classList.add('d-none'); }

    const firstName      = (document.getElementById('regFirstName') as HTMLInputElement).value.trim();
    const lastName       = (document.getElementById('regLastName') as HTMLInputElement).value.trim();
    const username       = (document.getElementById('regUsername') as HTMLInputElement).value.trim();
    const email          = (document.getElementById('regEmail') as HTMLInputElement).value.trim();
    const password       = (document.getElementById('regPassword') as HTMLInputElement).value;
    const passwordConfirm = (document.getElementById('regPasswordConfirm') as HTMLInputElement).value;

    if (!firstName || !lastName || !username || !email || !password) {
      showError(errorEl, 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    if (password !== passwordConfirm) {
      showError(errorEl, 'Mật khẩu xác nhận không khớp.');
      return;
    }
    if (password.length < 8) {
      showError(errorEl, 'Mật khẩu phải có ít nhất 8 ký tự.');
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      showError(errorEl, 'Địa chỉ email không hợp lệ.');
      return;
    }

    const submitBtn = form.querySelector<HTMLButtonElement>('[type="submit"]');
    const origText  = submitBtn?.textContent ?? '';
    if (submitBtn) { submitBtn.disabled = true; submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang đăng ký...'; }

    try {
      await register({ username, email, password, firstName, lastName });
      const redirect = new URLSearchParams(window.location.search).get('redirect');
      window.location.href = safeRedirect(redirect);
    } catch (err: any) {
      showError(errorEl, err.message);
      if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = origText; }
    }
  });
}

function initApp(): void {
  initI18n();
  initLoginForm();
  initRegisterForm();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
