/**
 * Shared teacher utilities: auth guard, formatters
 */
import type { EnrollmentStatus } from '../../src/shared/js/types.ts';

export function requireTeacher(): void {
  try {
    const user = JSON.parse(localStorage.getItem('sso_user') || 'null');
    if (!user || (user.role !== 'TEACHER' && user.role !== 'ADMIN')) {
      window.location.replace('/pages/login/?redirect=' + encodeURIComponent(window.location.pathname));
    }
  } catch {
    window.location.replace('/pages/login/');
  }
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function statusBadge(status: EnrollmentStatus | string | null | undefined): string {
  const map: Record<string, string> = {
    pending:    'bg-warning text-dark',
    active:     'bg-success',
    inprogress: 'bg-primary',
    completed:  'bg-info text-dark',
    revoked:    'bg-danger',
  };
  return `<span class="badge ${map[status as string] || 'bg-secondary'}">${status ?? '—'}</span>`;
}

export function progressBar(pct: number | null | undefined): string {
  return `<div class="progress" style="height:6px;min-width:70px">
    <div class="progress-bar ${pct === 100 ? 'bg-success' : ''}" style="width:${pct ?? 0}%"></div>
  </div><small class="text-muted">${pct ?? 0}%</small>`;
}
