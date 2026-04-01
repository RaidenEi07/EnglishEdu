/**
 * Toast notification system — replaces alert() calls with Bootstrap-styled toasts
 */

declare const bootstrap: { Toast: new (el: Element | null, opts?: Record<string, unknown>) => { show(): void } } | undefined;

type ToastType = 'success' | 'danger' | 'warning' | 'info';

const CONTAINER_ID = 'sso-toast-container';
const AUTO_DISMISS = 4000;

function getOrCreateContainer(): HTMLElement {
  let container = document.getElementById(CONTAINER_ID);
  if (!container) {
    container = document.createElement('div');
    container.id = CONTAINER_ID;
    container.className = 'toast-container position-fixed top-0 end-0 p-3';
    container.style.zIndex = '1090';
    document.body.appendChild(container);
  }
  return container;
}

const ICON_MAP: Record<ToastType, string> = {
  success: 'fa-circle-check',
  danger:  'fa-circle-xmark',
  warning: 'fa-triangle-exclamation',
  info:    'fa-circle-info',
};

const COLOR_MAP: Record<ToastType, string> = {
  success: 'text-success',
  danger:  'text-danger',
  warning: 'text-warning',
  info:    'text-info',
};

export function showToast(message: string, type: ToastType = 'info', duration: number = AUTO_DISMISS): void {
  const container = getOrCreateContainer();
  const id = `toast-${Date.now()}`;
  const icon = ICON_MAP[type] || ICON_MAP.info;
  const color = COLOR_MAP[type] || COLOR_MAP.info;

  const html = `
    <div id="${id}" class="toast align-items-center border-0 shadow-lg" role="alert" aria-live="assertive" aria-atomic="true">
      <div class="d-flex">
        <div class="toast-body d-flex align-items-center gap-2">
          <i class="fas ${icon} ${color} fs-5"></i>
          <span>${escapeHtml(message)}</span>
        </div>
        <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Đóng"></button>
      </div>
    </div>`;

  container.insertAdjacentHTML('beforeend', html);
  const toastEl = document.getElementById(id);
  if (!toastEl) return;

  if (typeof bootstrap !== 'undefined' && bootstrap?.Toast) {
    const bsToast = new bootstrap.Toast(toastEl, {
      autohide: duration > 0,
      delay: duration,
    });
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
    bsToast.show();
  } else {
    toastEl.classList.add('show');
    if (duration > 0) {
      setTimeout(() => toastEl.remove(), duration);
    }
  }
}

function escapeHtml(str: string): string {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export const toast = {
  success: (msg: string, ms?: number) => showToast(msg, 'success', ms),
  error: (msg: string, ms?: number) => showToast(msg, 'danger', ms),
  warning: (msg: string, ms?: number) => showToast(msg, 'warning', ms),
  info: (msg: string, ms?: number) => showToast(msg, 'info', ms),
};
