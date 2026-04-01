import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../src/shared/styles/shared.css';
import '../admin.css';
import 'bootstrap';
import { Modal } from 'bootstrap';

import { initI18n } from '../../../src/shared/js/i18n.ts';
import { initNavbar } from '../../../src/shared/js/navbar.ts';
import { injectNavbar } from '../../../src/shared/js/inject-navbar.ts';
import { injectFooter } from '../../../src/shared/js/footer.ts';
import { apiGet, apiPatch } from '../../../src/shared/js/api.ts';
import { toast } from '../../../src/shared/js/toast.ts';
import { requireAdmin, formatDate, statusBadge } from '../admin-utils.ts';
import type { AdminEnrollmentResponse, Page } from '../../../src/shared/js/types.ts';

let currentPage       = 0;
let totalPages        = 0;
let currentStatus     = 'pending';
let revokeModalInst: InstanceType<typeof Modal> | null   = null;
let pendingRevokeId: number | null   = null;

function renderRow(e: AdminEnrollmentResponse): string {
  const progress = `<div class="progress" style="height:6px;min-width:60px">
    <div class="progress-bar" style="width:${e.progress ?? 0}%"></div>
  </div><small>${e.progress ?? 0}%</small>`;

  const approveBtn = (e.status === 'pending')
    ? `<button class="btn btn-sm btn-success py-0 me-1" data-action="approve" data-id="${e.id}" title="Duyệt"><i class="fa fa-check"></i></button>`
    : '';
  const revokeBtn = (e.status !== 'revoked')
    ? `<button class="btn btn-sm btn-danger py-0" data-action="revoke" data-id="${e.id}" data-student="${escAttr(e.studentUsername)}" data-course="${escAttr(e.courseName)}" title="Thu hồi"><i class="fa fa-ban"></i></button>`
    : '';

  return `<tr>
    <td class="text-muted small">${e.id}</td>
    <td>
      <div class="fw-medium">${e.studentUsername ?? '—'}</div>
      <div class="text-muted small">${e.studentFullName ?? ''}</div>
    </td>
    <td class="small">${e.courseName ?? '—'}</td>
    <td>${statusBadge(e.status)}</td>
    <td class="d-none d-md-table-cell">${progress}</td>
    <td class="d-none d-lg-table-cell small">${formatDate(e.enrolledAt)}</td>
    <td>${approveBtn}${revokeBtn}</td>
  </tr>`;
}

function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
function escAttr(s: string | null | undefined): string {
  return String(s ?? '').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

async function loadEnrollments(page: number = 0): Promise<void> {
  const tbody = document.getElementById('enrollTableBody')!;
  tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';
  try {
    const qs   = currentStatus ? `status=${currentStatus}&` : '';
    const data = await apiGet<Page<AdminEnrollmentResponse>>(`/admin/enrollments?${qs}page=${page}&size=20`);
    currentPage = data.number;
    totalPages  = data.totalPages;

    tbody.innerHTML = data.content.length === 0
      ? '<tr><td colspan="7" class="text-center text-muted py-4">Không có đăng ký nào.</td></tr>'
      : data.content.map(renderRow).join('');

    tbody.querySelectorAll('[data-action]').forEach(btn => btn.addEventListener('click', handleAction));

    const pi = document.getElementById('pageInfo');
    if (pi) pi.textContent = `Trang ${currentPage + 1} / ${totalPages || 1} (${data.totalElements ?? 0} mục)`;
    (document.getElementById('prevPage') as HTMLButtonElement).disabled = currentPage === 0;
    (document.getElementById('nextPage') as HTMLButtonElement).disabled = currentPage >= totalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

function handleAction(e: Event): void {
  const btn    = e.currentTarget as HTMLElement;
  const action = btn.dataset.action;
  const id     = Number(btn.dataset.id);
  if (action === 'approve') approveEnrollment(id, btn as HTMLButtonElement);
  if (action === 'revoke')  openRevokeModal(id, btn as HTMLElement);
}

async function approveEnrollment(id: number, btn: HTMLButtonElement): Promise<void> {
  btn.disabled = true;
  try {
    await apiPatch(`/admin/enrollments/${id}/approve`);
    loadEnrollments(currentPage);
  } catch (err: any) {
    toast.error(err.message);
    btn.disabled = false;
  }
}

function openRevokeModal(id: number, btn: HTMLElement): void {
  pendingRevokeId = id;
  document.getElementById('revokeStudent')!.textContent = btn.dataset.student!;
  document.getElementById('revokeCourse')!.textContent  = btn.dataset.course!;
  (document.getElementById('revokeNote') as HTMLTextAreaElement).value          = '';
  revokeModalInst?.show();
}

async function confirmRevoke(): Promise<void> {
  if (!pendingRevokeId) return;
  const note    = (document.getElementById('revokeNote') as HTMLTextAreaElement).value.trim();
  const saveBtn = document.getElementById('confirmRevokeBtn') as HTMLButtonElement;
  saveBtn.disabled = true;
  try {
    const qs = note ? `?note=${encodeURIComponent(note)}` : '';
    await apiPatch(`/admin/enrollments/${pendingRevokeId}/revoke${qs}`);
    revokeModalInst?.hide();
    loadEnrollments(currentPage);
  } catch (err: any) {
    toast.error(err.message);
  } finally {
    saveBtn.disabled = false;
  }
}

function initApp(): void {
  requireAdmin();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();

  revokeModalInst = new Modal(document.getElementById('revokeModal')!);
  document.getElementById('confirmRevokeBtn')!.addEventListener('click', confirmRevoke);
  document.getElementById('prevPage')!.addEventListener('click', () => loadEnrollments(currentPage - 1));
  document.getElementById('nextPage')!.addEventListener('click', () => loadEnrollments(currentPage + 1));

  // Status tab clicks
  document.querySelectorAll<HTMLElement>('#statusTabs .nav-link').forEach(tab => {
    tab.addEventListener('click', (e: Event) => {
      e.preventDefault();
      document.querySelectorAll('#statusTabs .nav-link').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentStatus = tab.dataset.status!;
      loadEnrollments(0);
    });
  });

  // Read initial status from URL query param
  const urlStatus = new URLSearchParams(window.location.search).get('status');
  if (urlStatus) {
    currentStatus = urlStatus;
    document.querySelectorAll<HTMLElement>('#statusTabs .nav-link').forEach(t => {
      t.classList.toggle('active', t.dataset.status === urlStatus);
    });
  }

  loadEnrollments(0);
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
