/**
 * Home page - Course grid listing
 */
import { courseData } from '../shared/js/courseData.ts';
import { apiGet } from '../shared/js/api.ts';
import { isLoggedIn } from '../shared/js/auth.ts';
import type { CourseResponse, EnrolledCourseResponse, EnrollmentStatus, Page } from '../shared/js/types.ts';

let allCourses: CourseResponse[] = [];
const enrolledMap = new Map<number, EnrollmentStatus>();
const assignedSet = new Set<number>();

function enrollBtnHtml(courseId: number): string {
  if (!isLoggedIn()) {
    return `<a href="/pages/course/?id=${courseId}" class="btn btn-outline-primary btn-sm w-100"><i class="fa fa-info-circle me-1"></i>Xem & Đăng ký</a>`;
  }
  const status = enrolledMap.get(Number(courseId));
  if (status === 'pending') {
    return `<button class="btn btn-warning btn-sm w-100" disabled><i class="fa fa-clock me-1"></i>Chờ duyệt</button>`;
  }
  if (status === 'active' || status === 'inprogress') {
    return `<a href="/pages/my/courses/" class="btn btn-success btn-sm w-100"><i class="fa fa-play me-1"></i>Vào học</a>`;
  }
  if (status === 'completed') {
    return `<a href="/pages/my/courses/" class="btn btn-secondary btn-sm w-100"><i class="fa fa-check me-1"></i>Hoàn thành</a>`;
  }
  if (!assignedSet.has(Number(courseId))) {
    return `<button class="btn btn-outline-secondary btn-sm w-100" disabled><i class="fa fa-lock me-1"></i>Chưa được chỉ định</button>`;
  }
  if (status === 'revoked') {
    return `<a href="/pages/course/?id=${courseId}" class="btn btn-outline-secondary btn-sm w-100">Đăng ký lại</a>`;
  }
  return `<a href="/pages/course/?id=${courseId}" class="btn btn-outline-primary btn-sm w-100"><i class="fa fa-info-circle me-1"></i>Xem & Đăng ký</a>`;
}

function createCourseCard(course: CourseResponse): string {
  return `
    <div class="col-lg-3 col-md-4 col-sm-6 mb-4 course-item" data-category="${course.category}">
      <div class="course-card card shadow-sm h-100">
        <a href="/pages/course/?id=${course.id}" class="text-decoration-none text-dark">
          <div class="card-img-wrapper">
            <img src="${course.imageUrl || '/images/course-placeholder.svg'}" class="card-img-top" alt="${course.name}" loading="lazy">
            <span class="course-category">${course.category}</span>
          </div>
        </a>
        <div class="card-body d-flex flex-column">
          <h5 class="card-title"><a href="/pages/course/?id=${course.id}" class="text-decoration-none text-dark">${course.name}</a></h5>
          <div class="text-muted small mb-2">
            <i class="fas fa-signal me-1"></i>${course.level}
          </div>
          <div class="mt-auto">
            ${enrollBtnHtml(course.id)}
          </div>
        </div>
      </div>
    </div>`;
}

function renderCourses(filter: string): void {
  const container = document.getElementById('course-grid');
  if (!container) return;
  const filtered = (!filter || filter === 'ALL')
    ? allCourses
    : allCourses.filter((c) => c.category === filter);
  container.innerHTML = filtered.map(createCourseCard).join('');
}

function initFilters(): void {
  document.querySelectorAll<HTMLElement>('[data-filter]').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('[data-filter]').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const filter = (btn.getAttribute('data-filter') || 'ALL').toUpperCase();
      renderCourses(filter === 'ALL' ? 'ALL' : filter);
    });
  });
}

async function loadEnrolledMap(): Promise<void> {
  if (!isLoggedIn()) return;
  try {
    const list = await apiGet<EnrolledCourseResponse[] | Page<EnrolledCourseResponse>>('/courses/enrolled');
    enrolledMap.clear();
    const items = Array.isArray(list) ? list : (list as Page<EnrolledCourseResponse>).content ?? [];
    items.forEach((e) => {
      enrolledMap.set(Number(e.courseId), e.status);
    });
  } catch { /* ignore */ }
  try {
    const assigned = await apiGet<CourseResponse[]>('/courses/assigned');
    assignedSet.clear();
    assigned.forEach((c) => assignedSet.add(c.id));
  } catch { /* ignore */ }
}

export async function initCourses(): Promise<void> {
  if (!document.getElementById('course-grid')) return;
  try {
    const page = await apiGet<Page<CourseResponse>>('/courses?size=50');
    allCourses = page.content || (page as unknown as CourseResponse[]);
  } catch {
    allCourses = courseData as unknown as CourseResponse[];
  }
  await loadEnrolledMap();
  renderCourses('ALL');
  initFilters();
}
