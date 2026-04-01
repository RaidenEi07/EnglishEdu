/**
 * Forgot Password page entry point
 * Step 1: user provides username/email → receives reset email
 * Step 2: user lands on this page with ?token=... → sets new password
 */

/* --- Styles --- */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import './forgot-password.css';

/* --- Bootstrap JS --- */
import 'bootstrap';

/* --- Shared modules --- */
import { initI18n } from '../../src/shared/js/i18n.ts';
import { requestPasswordReset, resetPassword } from '../../src/shared/js/auth.ts';

/* ── Step 1: Search by username or email ─────────────────── */
function initStep1(): void {
  const form  = document.getElementById('forgotPasswordForm') as HTMLFormElement | null;
  const msgEl = document.getElementById('forgotMessage');
  if (!form) return;

  form.addEventListener('submit', async (e: Event) => {
    e.preventDefault();
    const username = (document.getElementById('searchUsername') as HTMLInputElement | null)?.value.trim();
    const email    = (document.getElementById('searchEmail') as HTMLInputElement | null)?.value.trim();
    const search   = username || email;
    if (!search) {
      if (msgEl) { msgEl.className = 'alert alert-warning mt-3'; msgEl.textContent = 'Vui lòng nhập tên đăng nhập hoặc email.'; msgEl.classList.remove('d-none'); }
      return;
    }
    const btn = form.querySelector('button[type="submit"]') as HTMLButtonElement | null;
    if (btn) { btn.disabled = true; btn.textContent = 'Đang gửi...'; }
    try {
      await requestPasswordReset(search);
      if (msgEl) {
        msgEl.className   = 'alert alert-success mt-3';
        msgEl.textContent = 'Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn.';
        msgEl.classList.remove('d-none');
      }
      form.reset();
    } catch (err: any) {
      if (msgEl) {
        msgEl.className   = 'alert alert-danger mt-3';
        msgEl.textContent = err.message;
        msgEl.classList.remove('d-none');
      }
    } finally {
      if (btn) { btn.disabled = false; btn.textContent = 'Đặt lại mật khẩu'; }
    }
  });
}

/* ── Step 2: Set new password with token from URL ─────────── */
function initStep2(token: string): void {
  // Show step-2 form, hide step-1
  document.getElementById('step1Section')?.classList.add('d-none');
  const step2 = document.getElementById('step2Section');
  step2?.classList.remove('d-none');

  const form  = document.getElementById('resetPasswordForm');
  const msgEl = document.getElementById('resetMessage');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const newPw     = (document.getElementById('newPassword') as HTMLInputElement | null)?.value;
    const confirmPw = (document.getElementById('confirmPassword') as HTMLInputElement | null)?.value;

    if (!newPw || newPw.length < 8) {
      if (msgEl) { msgEl.className = 'alert alert-warning mt-3'; msgEl.textContent = 'Mật khẩu phải có ít nhất 8 ký tự.'; msgEl.classList.remove('d-none'); }
      return;
    }
    if (newPw !== confirmPw) {
      if (msgEl) { msgEl.className = 'alert alert-warning mt-3'; msgEl.textContent = 'Mật khẩu xác nhận không khớp.'; msgEl.classList.remove('d-none'); }
      return;
    }

    const btn = form.querySelector('button[type="submit"]') as HTMLButtonElement | null;
    if (btn) { btn.disabled = true; btn.textContent = 'Đang xử lý...'; }
    try {
      await resetPassword(token, newPw!);
      if (msgEl) {
        msgEl.className   = 'alert alert-success mt-3';
        msgEl.textContent = 'Mật khẩu đã được đặt lại thành công. Đang chuyển đến trang đăng nhập...';
        msgEl.classList.remove('d-none');
      }
      form.style.display = 'none';
      setTimeout(() => { window.location.href = '/pages/login/'; }, 2000);
    } catch (err: any) {
      if (msgEl) {
        msgEl.className   = 'alert alert-danger mt-3';
        msgEl.textContent = err.message;
        msgEl.classList.remove('d-none');
      }
      if (btn) { btn.disabled = false; btn.textContent = 'Xác nhận'; }
    }
  });
}

function initApp(): void {
  initI18n();
  const token = new URLSearchParams(window.location.search).get('token');
  if (token) {
    initStep2(token);
  } else {
    initStep1();
  }
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
