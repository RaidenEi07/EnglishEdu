/**
 * Shared admin utilities: auth guard, formatters
 */
import { apiGet } from '../../src/shared/js/api.ts';
import type { Page, EnrollmentStatus } from '../../src/shared/js/types.ts';

/** Redirect non-admins to login. Call at the top of every admin page. */
export function requireAdmin(): void {
  try {
    const user = JSON.parse(localStorage.getItem('sso_user') || 'null');
    if (!user || user.role !== 'ADMIN') {
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

export async function fetchAdminStats(): Promise<{ totalUsers: number | string; totalCourses: number | string; pendingEnrollments: number | string }> {
  const [up, cp, ep] = await Promise.all([
    apiGet<Page<unknown>>('/admin/users?size=1').catch(() => ({ totalElements: '?' as number | string })),
    apiGet<Page<unknown>>('/admin/courses?size=1').catch(() => ({ totalElements: '?' as number | string })),
    apiGet<Page<unknown>>('/admin/enrollments?status=pending&size=1').catch(() => ({ totalElements: '?' as number | string })),
  ]);
  return {
    totalUsers:          up.totalElements   ?? 0,
    totalCourses:        cp.totalElements   ?? 0,
    pendingEnrollments:  ep.totalElements   ?? 0,
  };
}
