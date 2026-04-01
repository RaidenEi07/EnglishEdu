/**
 * Utilities shared between dashboard and my-courses pages
 */
import { apiGet } from '../../src/shared/js/api.ts';
import type { EnrolledCourseResponse, CourseResponse } from '../../src/shared/js/types.ts';

export async function fetchMyCourses(): Promise<EnrolledCourseResponse[]> {
  try {
    // Check if user is a teacher
    const raw = localStorage.getItem('sso_user');
    if (raw) {
      const user = JSON.parse(raw);
      if (user?.role === 'TEACHER') {
        // Fetch teacher's assigned courses and map to EnrolledCourseResponse shape
        const courses = await apiGet<{ content: CourseResponse[] }>('/teacher/courses?page=0&size=100');
        return (courses.content || []).map((c: CourseResponse) => ({
          enrollmentId: 0,
          courseId:     c.id,
          externalId:   null,
          name:         c.name,
          category:     c.category ?? '',
          level:        c.level ?? '',
          imageUrl:     c.imageUrl ?? null,
          status:       'active' as const,
          progress:     0,
          enrolledAt:   c.createdAt,
          lastAccessed: null,
          starred:      false,
          hidden:       false,
          teacherNote:  null,
        }));
      }
    }
    return await apiGet<EnrolledCourseResponse[]>('/courses/enrolled');
  } catch { return []; }
}

export function setUserInitials(): void {
  const el = document.getElementById('userInitials');
  if (!el) return;
  try {
    const raw = localStorage.getItem('sso_user');
    if (raw) {
      const user  = JSON.parse(raw);
      const first = (user.firstName  || user.first_name  || '').charAt(0).toUpperCase();
      const last  = (user.lastName   || user.last_name   || '').charAt(0).toUpperCase();
      el.textContent = (first + last) || '?';
    }
  } catch { /* ignore */ }
}
