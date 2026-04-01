/**
 * Manage page — Admin enrollment approval
 * Only accessible to ADMIN role
 */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import '../admin/admin.css';
import 'bootstrap';
import { Modal } from 'bootstrap';

import { initI18n } from '../../src/shared/js/i18n.ts';
import { initNavbar } from '../../src/shared/js/navbar.ts';
import { injectFooter } from '../../src/shared/js/footer.ts';
import { injectNavbar } from '../../src/shared/js/inject-navbar.ts';
import { isLoggedIn } from '../../src/shared/js/auth.ts';
import { apiGet, apiPatch } from '../../src/shared/js/api.ts';
import { toast } from '../../src/shared/js/toast.ts';
import type { AdminEnrollmentResponse, Page } from '../../src/shared/js/types.ts';

/* ── Auth guard ─────────────────────────────────────────── */
function requireAdmin(): void {
  if (!isLoggedIn()) {
    window.location.replace('/pages/login/?redirect=' + encodeURIComponent(window.location.pathname));
    return;
  }
  try {
    const user = JSON.parse(localStorage.getItem('sso_user') || 'null');
    if (!user || user.role !== 'ADMIN') {
      window.location.replace('/pages/login/?redirect=' + encodeURIComponent(window.location.pathname));
    }
  } catch {
    window.location.replace('/pages/login/');
  }
}

/* ── State ──────────────────────────────────────────────── */
let currentPage   = 0;
let totalPages    = 0;
let currentStatus = 'pending';
let revokeModal: InstanceType<typeof Modal> | null = null;
let pendingRevokeId: number | null = null;

/* ── Helpers ────────────────────────────────────────────── */
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
function escAttr(s: string | null | undefined): string {
  return String(s ?? '').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

const STATUS_LABELS: Record<string, string> = {
  pending:    '<span class="badge bg-warning text-dark">Chờ duyệt</span>',
  active:     '<span class="badge bg-success">Đang học</span>',
  inprogress: '<span class="badge bg-primary">Đang học</span>',
  completed:  '<span class="badge bg-info text-dark">Hoàn thành</span>',
  revoked:    '<span class="badge bg-danger">Đã thu hồi</span>',
};

function statusBadge(s: string | null | undefined): string {
  return STATUS_LABELS[s ?? ''] || `<span class="badge bg-secondary">${s ?? '—'}</span>`;
}

/* ── Render ─────────────────────────────────────────────── */
function renderRow(e: AdminEnrollmentResponse): string {
  const approveBtn = (e.status === 'pending')
    ? `<button class="btn btn-sm btn-success py-0 me-1" data-action="approve" data-id="${e.id}" title="Duyệt">
         <i class="fa fa-check me-1"></i>Duyệt
       </button>`
    : '';
  const revokeBtn = (e.status !== 'revoked')
    ? `<button class="btn btn-sm btn-outline-danger py-0" data-action="revoke" data-id="${e.id}"
         data-student="${escAttr(e.studentUsername)}" data-course="${escAttr(e.courseName)}" title="Thu hồi">
         <i class="fa fa-ban"></i>
       </button>`
    : '';
  const dateToShow = formatDate(e.requestDate || e.enrolledAt);
  return `<tr>
    <td class="text-muted small align-middle">${e.id}</td>
    <td class="align-middle">
      <div class="fw-medium">${e.studentUsername ?? '—'}</div>
      <div class="text-muted small">${e.studentFullName ?? ''}</div>
    </td>
    <td class="small align-middle">${e.courseName ?? '—'}</td>
    <td class="small align-middle">${dateToShow}</td>
    <td class="align-middle">${statusBadge(e.status)}</td>
    <td class="align-middle">${approveBtn}${revokeBtn}</td>
  </tr>`;
}

/* ── Data loading ───────────────────────────────────────── */
async function loadEnrollments(page: number = 0): Promise<void> {
  const tbody = document.getElementById('enrollTableBody')!;
  tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4"><span class="spinner-border spinner-border-sm"></span></td></tr>';

  const titleMap: Record<string, string> = {
    pending:    'Danh sách chờ duyệt',
    active:     'Danh sách đang học',
    inprogress: 'Danh sách đang học',
    '':         'Tất cả đăng ký',
  };
  const tableTitle = document.getElementById('tableTitle');
  if (tableTitle) tableTitle.textContent = titleMap[currentStatus] ?? 'Danh sách đăng ký';

  try {
    const qs   = currentStatus ? `status=${currentStatus}&` : '';
    const data = await apiGet<Page<AdminEnrollmentResponse>>(`/admin/enrollments?${qs}page=${page}&size=20`);
    currentPage = data.number;
    totalPages  = data.totalPages;

    tbody.innerHTML = data.content.length === 0
      ? '<tr><td colspan="6" class="text-center text-muted py-4"><i class="fa fa-inbox me-2"></i>Không có đăng ký nào.</td></tr>'
      : data.content.map(renderRow).join('');

    tbody.querySelectorAll('[data-action]').forEach((btn) =>
      btn.addEventListener('click', handleAction));

    const pageLabel = document.getElementById('pageLabel');
    if (pageLabel) pageLabel.textContent = `Trang ${currentPage + 1} / ${Math.max(1, totalPages)} (${data.totalElements ?? 0} mục)`;
    (document.getElementById('prevPage') as HTMLButtonElement).disabled = currentPage === 0;
    (document.getElementById('nextPage') as HTMLButtonElement).disabled = currentPage >= totalPages - 1;
  } catch (err: any) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">${escHtml(err.message)}</td></tr>`;
  }
}

async function loadStats(): Promise<void> {
  try {
    const [pendingPage, activePage, allPage] = await Promise.all([
      apiGet<Page<unknown>>('/admin/enrollments?status=pending&size=1').catch(() => ({ totalElements: '?' })),
      apiGet<Page<unknown>>('/admin/enrollments?status=active&size=1').catch(() => ({ totalElements: '?' })),
      apiGet<Page<unknown>>('/admin/enrollments?size=1').catch(() => ({ totalElements: '?' })),
    ]);
    const pe = document.getElementById('statPending');    if (pe) pe.textContent = String(pendingPage.totalElements ?? 0);
    const ae = document.getElementById('statActive');     if (ae) ae.textContent = String(activePage.totalElements  ?? 0);
    const te = document.getElementById('statTotal');      if (te) te.textContent = String(allPage.totalElements     ?? 0);
    const pb = document.getElementById('pendingBadge');   if (pb && Number(pendingPage.totalElements ?? 0) > 0) pb.textContent = String(pendingPage.totalElements);
  } catch { /* noop */ }
}

/* ── Actions ────────────────────────────────────────────── */
function handleAction(e: Event): void {
  const btn    = e.currentTarget as HTMLElement;
  const action = btn.dataset.action;
  const id     = Number(btn.dataset.id);
  if (action === 'approve') approveEnrollment(id, btn as HTMLButtonElement);
  if (action === 'revoke')  openRevokeModal(id, btn);
}

async function approveEnrollment(id: number, btn: HTMLButtonElement): Promise<void> {
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
  try {
    await apiPatch(`/admin/enrollments/${id}/approve`);
    toast.success('Đã duyệt đăng ký!');
    await Promise.all([loadEnrollments(currentPage), loadStats()]);
  } catch (err: any) {
    toast.error(err.message);
    btn.disabled = false;
    btn.innerHTML = '<i class="fa fa-check me-1"></i>Duyệt';
  }
}

function openRevokeModal(id: number, btn: HTMLElement): void {
  pendingRevokeId = id;
  const rsEl = document.getElementById('revokeStudent');
  const rcEl = document.getElementById('revokeCourse');
  if (rsEl) rsEl.textContent = btn.dataset.student ?? '';
  if (rcEl) rcEl.textContent = btn.dataset.course  ?? '';
  const noteEl = document.getElementById('revokeNote') as HTMLTextAreaElement | null;
  if (noteEl) noteEl.value = '';
  revokeModal?.show();
}

async function confirmRevoke(): Promise<void> {
  if (!pendingRevokeId) return;
  const note    = (document.getElementById('revokeNote') as HTMLTextAreaElement).value.trim();
  const saveBtn = document.getElementById('confirmRevokeBtn') as HTMLButtonElement;
  saveBtn.disabled = true;
  try {
    const qs = note ? `?note=${encodeURIComponent(note)}` : '';
    await apiPatch(`/admin/enrollments/${pendingRevokeId}/revoke${qs}`);
    revokeModal?.hide();
    toast.success('Đã thu hồi đăng ký.');
    await Promise.all([loadEnrollments(currentPage), loadStats()]);
  } catch (err: any) {
    toast.error(err.message);
  } finally {
    saveBtn.disabled = false;
  }
}

/* ── Init ───────────────────────────────────────────────── */
function initApp(): void {
  requireAdmin();
  injectNavbar();
  injectFooter();
  initI18n();
  initNavbar();

  // Revoke modal
  revokeModal = new Modal(document.getElementById('revokeModal')!);
  document.getElementById('confirmRevokeBtn')!.addEventListener('click', confirmRevoke);

  // Pagination
  document.getElementById('prevPage')!.addEventListener('click', () => loadEnrollments(currentPage - 1));
  document.getElementById('nextPage')!.addEventListener('click', () => loadEnrollments(currentPage + 1));

  // Refresh
  document.getElementById('refreshBtn')?.addEventListener('click', () => {
    loadEnrollments(currentPage);
    loadStats();
  });

  // Status tabs
  document.querySelectorAll<HTMLElement>('[data-status]').forEach((tab) => {
    tab.addEventListener('click', (e) => {
      e.preventDefault();
      document.querySelectorAll('[data-status]').forEach((t) => t.classList.remove('active'));
      tab.classList.add('active');
      currentStatus = tab.dataset.status!;
      loadEnrollments(0);
    });
  });

  loadStats();
  loadEnrollments(0);
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
