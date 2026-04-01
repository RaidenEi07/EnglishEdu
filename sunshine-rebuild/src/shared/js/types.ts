/* ============================================
   API Response Types
   ============================================ */

export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

/* ── Auth ──────────────────────────────────── */
export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface CreateAdminUserRequest {
  username: string;
  password: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
}

export interface UserResponse {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  active: boolean;
  guest: boolean;
  moodleId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface StoredUser {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  role: 'STUDENT' | 'TEACHER' | 'ADMIN';
  guest: boolean;
}

/* ── Course ────────────────────────────────── */
export interface CourseResponse {
  id: number;
  externalId: number | null;
  name: string;
  category: string;
  level: string;
  description: string | null;
  imageUrl: string | null;
  teacherId: number | null;
  teacherName: string | null;
  published: boolean;
  moodleCourseId: number | null;
  createdAt: string;
}

export type EnrollmentStatus = 'pending' | 'active' | 'inprogress' | 'completed' | 'revoked';

export interface EnrolledCourseResponse {
  enrollmentId: number;
  courseId: number;
  externalId: number | null;
  name: string;
  category: string;
  level: string;
  imageUrl: string | null;
  progress: number;
  status: EnrollmentStatus;
  enrolledAt: string;
  lastAccessed: string | null;
  starred: boolean;
  hidden: boolean;
  teacherNote: string | null;
}

export interface DashboardResponse {
  totalEnrolled: number;
  pendingCount: number;
  inProgress: number;
  completed: number;
}

/* ── Course Assignment ─────────────────────── */
export interface CourseAssignmentResponse {
  id: number;
  userId: number;
  username: string;
  studentFullName: string;
  courseId: number;
  courseName: string;
  courseCategory: string;
  assignedByUsername: string | null;
  assignedAt: string;
}

export interface AssignCoursesRequest {
  userId: number;
  courseIds: number[];
}

/* ── Admin ─────────────────────────────────── */
export interface AdminEnrollmentResponse {
  id: number;
  courseId: number | null;
  courseName: string;
  studentId: number | null;
  studentUsername: string;
  studentFirstName: string | null;
  studentLastName: string | null;
  studentFullName: string;
  status: EnrollmentStatus;
  progress: number;
  requestDate: string | null;
  enrolledAt: string;
  approvedAt: string | null;
  approvedByUsername: string | null;
  expiryDate: string | null;
  teacherNote: string | null;
}

/* ── Teacher ───────────────────────────────── */
export interface TeacherDashboardResponse {
  totalCourses: number;
  totalStudents: number;
  pendingStudents: number;
  activeStudents: number;
}

export interface TeacherEnrollmentResponse {
  id: number;
  studentUsername: string;
  studentFirstName: string;
  studentLastName: string;
  courseName: string;
  status: EnrollmentStatus;
  progress: number;
  enrolledAt: string;
  expiryDate: string | null;
  teacherNote: string | null;
}

/* ── Notification ──────────────────────────── */
export interface NotificationResponse {
  id: number;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

/* ── Pagination ────────────────────────────── */
export interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

/* ── Course static data ────────────────────── */
export interface CourseStaticData {
  id: number;
  name: string;
  category: string;
  level: string;
}

/* ── Moodle ────────────────────────────────── */
export interface MoodleLaunchResponse {
  url: string;
  moodleBaseUrl?: string;
}
