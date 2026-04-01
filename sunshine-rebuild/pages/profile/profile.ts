/**
 * Profile page entry point
 */

/* --- Styles --- */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import './profile.css';

/* --- Bootstrap JS --- */
import 'bootstrap';

/* --- Shared modules --- */
import { initI18n } from '../../src/shared/js/i18n.ts';
import { initNavbar } from '../../src/shared/js/navbar.ts';
import { injectFooter } from '../../src/shared/js/footer.ts';
import { injectNavbar } from '../../src/shared/js/inject-navbar.ts';
import { isLoggedIn, logout } from '../../src/shared/js/auth.ts';
import { apiGet, apiPut, apiPost, apiUpload } from '../../src/shared/js/api.ts';
import { toast } from '../../src/shared/js/toast.ts';
import type { UserResponse } from '../../src/shared/js/types.ts';

function showProfileAlert(message: string, type: string = 'danger'): void {
  if (type === 'success') toast.success(message);
  else toast.error(message);
}

async function fetchProfile(): Promise<UserResponse | null>                           { try { return await apiGet<UserResponse>('/users/me'); } catch { return null; } }
async function updateProfile(data: Record<string, string | undefined>): Promise<unknown>  { return apiPut('/users/me', data); }
async function changePassword(currentPassword: string, newPw: string): Promise<unknown>   { return apiPost('/users/me/password', { currentPassword, newPassword: newPw }); }

function populatePage(profile: UserResponse | null): void {
  if (!profile) return;
  const set    = (id: string, val: string | null | undefined) => { const el = document.getElementById(id); if (el) el.textContent = val ?? '—'; };
  const setVal = (id: string, val: string | null | undefined) => { const el = document.getElementById(id) as HTMLInputElement | null; if (el) el.value = val ?? ''; };
  const setSrc = (id: string, src: string | null | undefined) => { const el = document.getElementById(id) as HTMLImageElement | null; if (el && src) el.src = src; };
  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ') || 'Học viên';
  set('profileDisplayName', fullName);
  set('profileUsername',    `@${profile.username || ''}`);
  set('profileEmail',       profile.email);
  set('profileJoined',      profile.createdAt ? new Date(profile.createdAt).toLocaleDateString('vi-VN') : '—');
  setSrc('profileAvatar',   profile.avatarUrl);
  setSrc('userAvatar',      profile.avatarUrl);
  set('userName', fullName);
  setVal('inputFirstName',  profile.firstName);
  setVal('inputLastName',   profile.lastName);
  setVal('inputEmail',      profile.email);
  setVal('inputUsername',   profile.username);
}

async function initProfile(): Promise<void> {
  if (!document.getElementById('profileForm')) return;
  if (!isLoggedIn()) {
    window.location.replace(`/pages/login/?redirect=${encodeURIComponent(window.location.pathname)}`);
    return;
  }
  const profile = await fetchProfile();
  populatePage(profile);

  // Update footer user link with fresh API data (injectFooter sets initial value from localStorage)
  const footerLink = document.getElementById('footerUserLink');
  if (profile && footerLink) {
    footerLink.textContent = [profile.firstName, profile.lastName].filter(Boolean).join(' ') || profile.username;
  }

  // Profile info form
  const profileForm = document.getElementById('profileForm');
  profileForm?.addEventListener('submit', async (e: Event) => {
    e.preventDefault();
    const btn  = profileForm.querySelector('[type="submit"]') as HTMLButtonElement | null;
    const orig = btn?.textContent;
    if (btn) { btn.disabled = true; btn.textContent = 'Đang lưu...'; }
    try {
      await updateProfile({
        firstName: (document.getElementById('inputFirstName') as HTMLInputElement | null)?.value.trim(),
        lastName:  (document.getElementById('inputLastName') as HTMLInputElement | null)?.value.trim(),
        email:     (document.getElementById('inputEmail') as HTMLInputElement | null)?.value.trim(),
      });
      showProfileAlert('Cập nhật thông tin thành công!', 'success');
    } catch (err: any) {
      showProfileAlert(err.message);
    } finally {
      if (btn) { btn.disabled = false; btn.textContent = orig ?? ''; }
    }
  });

  // Change password form
  const pwForm  = document.getElementById('changePasswordForm');
  const pwError = document.getElementById('pwError');
  pwForm?.addEventListener('submit', async (e: Event) => {
    e.preventDefault();
    pwError?.classList.add('d-none');
    const current = (document.getElementById('currentPassword') as HTMLInputElement | null)?.value;
    const newPw   = (document.getElementById('newPassword') as HTMLInputElement | null)?.value;
    const confirm = (document.getElementById('confirmPassword') as HTMLInputElement | null)?.value;
    if (!current || !newPw || !confirm) {
      if (pwError) { pwError.textContent = 'Vui lòng điền đầy đủ các trường.'; pwError.classList.remove('d-none'); } return;
    }
    if (newPw !== confirm) {
      if (pwError) { pwError.textContent = 'Mật khẩu xác nhận không khớp.'; pwError.classList.remove('d-none'); } return;
    }
    if (newPw.length < 8) {
      if (pwError) { pwError.textContent = 'Mật khẩu mới phải có ít nhất 8 ký tự.'; pwError.classList.remove('d-none'); } return;
    }
    const btn  = pwForm.querySelector('[type="submit"]') as HTMLButtonElement | null;
    const orig = btn?.textContent;
    if (btn) { btn.disabled = true; btn.textContent = 'Đang xử lý...'; }
    try {
      await changePassword(current!, newPw!);
      showProfileAlert('Đổi mật khẩu thành công! Vui lòng đăng nhập lại.', 'success');
      (pwForm as HTMLFormElement).reset();
      setTimeout(() => logout(), 3000);
    } catch (err: any) {
      if (pwError) { pwError.textContent = err.message; pwError.classList.remove('d-none'); }
    } finally {
      if (btn) { btn.disabled = false; btn.textContent = orig ?? ''; }
    }
  });

  // Password visibility toggles
  document.querySelectorAll<HTMLElement>('[data-pw-toggle]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const input = document.getElementById(btn.dataset.pwToggle!) as HTMLInputElement | null;
      if (!input) return;
      const isHidden = input.type === 'password';
      input.type = isHidden ? 'text' : 'password';
      btn.querySelector('i')?.classList.toggle('fa-eye',      !isHidden);
      btn.querySelector('i')?.classList.toggle('fa-eye-slash', isHidden);
    });
  });

  // Avatar upload
  const avatarUpload = document.getElementById('avatarUpload') as HTMLInputElement | null;
  avatarUpload?.addEventListener('change', async () => {
    const file = avatarUpload.files?.[0];
    if (!file || !file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const src = ev.target?.result as string;
      const profileAv = document.getElementById('profileAvatar') as HTMLImageElement | null;
      const navAv     = document.getElementById('userAvatar') as HTMLImageElement | null;
      if (profileAv) profileAv.src = src;
      if (navAv)     navAv.src     = src;
    };
    reader.readAsDataURL(file);
    try {
      const formData = new FormData();
      formData.append('file', file);
      await apiUpload('/users/me/avatar', formData);
      showProfileAlert('Ảnh đại diện đã được cập nhật!', 'success');
    } catch (err: any) {
      showProfileAlert(err.message || 'Tải ảnh đại diện thất bại.');
    }
  });
}

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initProfile();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
