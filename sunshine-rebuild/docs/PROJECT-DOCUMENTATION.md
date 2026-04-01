# Tài liệu dự án EnglishEdu

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Công nghệ sử dụng](#2-công-nghệ-sử-dụng)
3. [Phân quyền hệ thống](#3-phân-quyền-hệ-thống)
4. [Luồng hoạt động & API](#4-luồng-hoạt-động--api)
5. [Danh sách API chi tiết](#5-danh-sách-api-chi-tiết)
6. [Cơ sở dữ liệu](#6-cơ-sở-dữ-liệu)
7. [Frontend - Danh sách trang](#7-frontend---danh-sách-trang)

---

## 1. Tổng quan kiến trúc

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│    Frontend (Vite + TS)     │────▶│   Backend (Spring Boot)     │
│    sunshine-rebuild/        │ API │   EnglishEdu_BE/            │
│    Port: 5173               │────▶│   Port: 4000                │
│                             │◀═══▶│                             │
│                             │ WS  │   (STOMP + SockJS)          │
└─────────────────────────────┘     └──────────┬──────────────────┘
                                               │
                              ┌────────────────┼────────────────┐
                              │                │                │
                        ┌─────▼─────┐   ┌──────▼─────┐  ┌──────▼─────┐
                        │ PostgreSQL │   │   Redis    │  │   MinIO    │
                        │ (Database) │   │ (JWT Cache)│  │ (Storage)  │
                        └───────────┘   └────────────┘  └────────────┘
```

- **Frontend**: Vite multi-page app (TypeScript + Bootstrap 5), tương tác qua REST API và WebSocket
- **Backend**: Spring Boot (Java), JWT authentication, Flyway migration, WebSocket (STOMP + SockJS)
- **Database**: PostgreSQL — lưu trữ users, courses, enrollments, notifications, calendar, timeline, payments, reviews, modules, lessons
- **Redis**: Blacklist JWT token khi logout
- **MinIO**: S3-compatible, lưu trữ avatar người dùng
- **WebSocket**: STOMP over SockJS — real-time push notifications (enrollment, payment)

---

## 2. Công nghệ sử dụng

### Backend
| Thành phần | Công nghệ |
|---|---|
| Framework | Spring Boot (Java) |
| ORM | JPA / Hibernate |
| Database | PostgreSQL |
| Migration | Flyway |
| Xác thực | JWT (HS256, 24h) |
| Cache | Redis |
| File Storage | MinIO (S3) |
| Email | JavaMail + Gmail SMTP |
| WebSocket | spring-boot-starter-websocket (STOMP + SockJS) |
| API Docs | Swagger / OpenAPI 3.0 |
| Mapping | MapStruct |

### Frontend
| Thành phần | Công nghệ |
|---|---|
| Build Tool | Vite |
| Ngôn ngữ | TypeScript |
| UI Framework | Bootstrap 5 |
| Icons | Font Awesome 6 |
| Đa ngôn ngữ | i18n (vi/en) |
| State | localStorage (JWT token + user) |

---

## 3. Phân quyền hệ thống

### Vai trò (Roles)

| Vai trò | Mô tả | Quyền |
|---|---|---|
| **STUDENT** | Học viên (mặc định khi đăng ký) | Xem khóa học, đăng ký khóa học, xem dashboard cá nhân, cập nhật profile |
| **TEACHER** | Giáo viên | Tất cả quyền STUDENT + quản lý học viên trong khóa học được giao, xem thống kê giảng dạy |
| **ADMIN** | Quản trị viên | Tất cả quyền TEACHER + quản lý toàn bộ users/courses/enrollments, duyệt đăng ký |

### Cấu hình bảo mật (Security Filter Chain)

```
Endpoint                              Quyền truy cập
────────────────────────────────────  ─────────────────────
/api/v1/auth/**                       Công khai (không cần đăng nhập)
GET /api/v1/courses                   Công khai
GET /api/v1/courses/{id}              Công khai
GET /api/v1/categories                Công khai
GET /api/v1/categories/{id}           Công khai
GET /api/v1/levels                    Công khai
GET /api/v1/levels/{id}               Công khai
GET /api/v1/courses/{courseId}/modules Công khai
GET /api/v1/courses/{courseId}/reviews Công khai
/ws/**                                Công khai (WebSocket)
/swagger-ui/**, /v3/api-docs/**       Công khai
/actuator/health                      Công khai
/api/v1/admin/**                      Chỉ ADMIN
/api/v1/teacher/**                    TEACHER hoặc ADMIN
Tất cả endpoint còn lại              Cần đăng nhập (Authenticated)
```

### Cơ chế xác thực
1. Người dùng đăng nhập → nhận JWT token
2. Frontend lưu token vào `localStorage` (`sso_token`)
3. Mọi request API gửi kèm header: `Authorization: Bearer {token}`
4. Khi logout, token được thêm vào Redis blacklist

---

## 4. Luồng hoạt động & API

### 4.1. Luồng đăng nhập (Login)

**Phân quyền**: Công khai (bất kỳ ai)

```
Người dùng ──▶ Trang Login ──▶ Nhập username/password
                                      │
                              POST /auth/login
                                      │
                                      ▼
                              Nhận JWT token + UserResponse
                                      │
                              Lưu vào localStorage
                                      │
                                      ▼
                              Chuyển hướng về trang chủ
```

| API | Method | Mô tả |
|---|---|---|
| `/auth/login` | POST | Đăng nhập với username/password → trả về `{token, user}` |
| `/auth/guest` | POST | Đăng nhập dạng khách (guest) → trả về `{token, user}` |
| `/auth/logout` | POST | Đăng xuất, vô hiệu hóa token (cần Authorization header) |

---

### 4.2. Luồng quên mật khẩu (Forgot Password)

**Phân quyền**: Công khai

```
Người dùng ──▶ Trang Quên mật khẩu ──▶ Nhập email
                                              │
                                    POST /auth/forgot-password
                                              │
                                              ▼
                                    Nhận email có link reset
                                              │
                                    Click link (có token)
                                              │
                                    POST /auth/reset-password
                                              │
                                              ▼
                                    Mật khẩu đã được đổi
```

| API | Method | Mô tả |
|---|---|---|
| `/auth/forgot-password` | POST | Gửi email reset password (`{search: email}`) |
| `/auth/reset-password` | POST | Đặt lại mật khẩu (`{token, newPassword}`) |

---

### 4.3. Luồng duyệt khóa học (Browse Courses)

**Phân quyền**: Công khai (hiển thị tất cả khóa học đã publish)

```
Người dùng ──▶ Trang chủ ──▶ Xem danh sách khóa học
                                      │
                             GET /courses?size=50
                                      │
                              (Nếu đã đăng nhập)
                             GET /courses/enrolled ──▶ Hiển thị trạng thái đăng ký
                                      │
                              Click vào khóa học
                                      │
                                      ▼
                             GET /courses/{id} ──▶ Trang chi tiết khóa học
```

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/courses` | GET | Công khai | Danh sách khóa học (phân trang, lọc theo category) |
| `/courses/{id}` | GET | Công khai | Chi tiết một khóa học |
| `/courses/enrolled` | GET | Authenticated | Danh sách khóa học đã đăng ký của user |

---

### 4.4. Luồng đăng ký khóa học (Enrollment)

**Phân quyền**: STUDENT trở lên (chỉ STUDENT mới có nút đăng ký)

#### Khóa học MIỄN PHÍ (is_free = TRUE)

```
Học viên ──▶ Trang chi tiết khóa học ──▶ Nhấn "Đăng ký"
                                                │
                                     POST /courses/{id}/enroll
                                                │
                                                ▼
                                     Enrollment tạo với status = "active" (tự động kích hoạt)
                                                │
                                                ▼
                                     Học viên được học ngay lập tức
```

#### Khóa học CÓ PHÍ (is_free = FALSE)

```
Học viên ──▶ Trang chi tiết khóa học ──▶ Nhấn "Đăng ký"
                                                │
                                     POST /courses/{id}/enroll
                                                │
                                                ▼
                                     Enrollment tạo với status = "pending"
                                                │
                              ┌─────────────────┴─────────────────┐
                              │                                   │
                     Thanh toán                          Admin duyệt thủ công
                              │                                   │
                POST /payments/initiate              PATCH /admin/enrollments/{id}/approve
                              │                                   │
                    Callback xác nhận                             │
                              │                                   │
                              ▼                                   ▼
                     status = "active"                   status = "active"
                     (Được học)                          (Được học)
                
                              Hoặc: PATCH /admin/enrollments/{id}/revoke → status = "revoked"
```

> **Lưu ý**: Progress được tự động tính dựa trên lesson_progress (số bài đã hoàn thành / tổng số bài)

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/courses/{id}/enroll` | POST | STUDENT+ | Tạo enrollment mới (auto-active nếu miễn phí, pending nếu có phí) |
| `/courses/{id}/enrollment` | PATCH | STUDENT+ | Cập nhật tiến độ học (`{progress: 0-100}`) |
| `/admin/enrollments/{id}/approve` | PATCH | ADMIN | Duyệt enrollment → active |
| `/admin/enrollments/{id}/revoke` | PATCH | ADMIN | Từ chối/thu hồi enrollment → revoked |

---

### 4.5. Luồng quản lý cá nhân (My Dashboard & My Courses)

**Phân quyền**: Authenticated (STUDENT+)

```
Học viên ──▶ Dashboard ──▶ Xem khóa học gần đây + sự kiện sắp tới
                │
                ├── GET /courses/recent
                ├── GET /courses/dashboard
                └── GET /timeline?days=7

Học viên ──▶ My Courses ──▶ Xem tất cả khóa học đã đăng ký
                │
                ├── GET /courses/enrolled
                └── PATCH /courses/{id}/enrollment (cập nhật progress)
```

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/courses/recent` | GET | Authenticated | Khóa học truy cập gần đây |
| `/courses/dashboard` | GET | Authenticated | Thống kê enrollment (tiến độ, hoàn thành) |
| `/timeline` | GET | Authenticated | Sự kiện sắp tới (`?days=7&sort=upcoming`) |
| `/courses/enrolled` | GET | Authenticated | Tất cả khóa học đã đăng ký |
| `/courses/{id}/enrollment` | PATCH | Authenticated | Cập nhật progress |

---

### 4.6. Luồng quản lý hồ sơ (Profile)

**Phân quyền**: Authenticated (tất cả role)

```
Người dùng ──▶ Trang Profile ──▶ Xem/Sửa thông tin cá nhân
                                        │
                              GET /users/me ──▶ Load thông tin
                                        │
                    ┌───────────────────┼────────────────────┐
                    │                   │                    │
           PUT /users/me        POST /users/me/password  POST /users/me/avatar
           (Cập nhật tên,       (Đổi mật khẩu)          (Upload ảnh đại diện)
            email)
```

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/users/me` | GET | Authenticated | Lấy thông tin profile |
| `/users/me` | PUT | Authenticated | Cập nhật firstName, lastName, email |
| `/users/me/password` | POST | Authenticated | Đổi mật khẩu (`{currentPassword, newPassword}`) |
| `/users/me/avatar` | POST | Authenticated | Upload avatar (multipart/form-data) |

---

### 4.7. Luồng giáo viên (Teacher)

**Phân quyền**: TEACHER hoặc ADMIN

```
Giáo viên ──▶ Teacher Dashboard ──▶ Xem thống kê + khóa học được giao
                      │
                      ├── GET /teacher/dashboard
                      └── GET /teacher/courses?page=0&size=12
                                │
                                ▼
              Teacher Students ──▶ Quản lý học viên trong khóa học
                      │
                      ├── GET /teacher/courses?size=100 (dropdown lọc)
                      ├── GET /teacher/enrollments?courseId=&status=&page=0&size=20
                      └── PATCH /teacher/enrollments/{id}
                             (Cập nhật: progress, status, teacherNote, expiryDate)
```

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/teacher/dashboard` | GET | TEACHER/ADMIN | Thống kê giáo viên (số học viên, hoàn thành, chờ duyệt) |
| `/teacher/courses` | GET | TEACHER/ADMIN | Khóa học được giao (phân trang) |
| `/teacher/enrollments` | GET | TEACHER/ADMIN | Danh sách enrollment theo khóa học và trạng thái |
| `/teacher/enrollments/{id}` | PATCH | TEACHER/ADMIN | Cập nhật enrollment (progress, status, note, expiry) |

---

### 4.8. Luồng quản trị viên (Admin)

**Phân quyền**: Chỉ ADMIN

#### 4.8.1. Quản lý người dùng

```
Admin ──▶ Admin Users ──▶ Danh sách tất cả người dùng
                │
                ├── GET /admin/users?role=&page=0&size=20
                ├── PATCH /admin/users/{id}/role (Đổi vai trò)
                └── PATCH /admin/users/{id}/active (Khóa/mở tài khoản)
```

| API | Method | Mô tả |
|---|---|---|
| `/admin/users` | GET | Danh sách users (lọc theo role, phân trang) |
| `/admin/users/{id}` | GET | Chi tiết user |
| `/admin/users/{id}/role` | PATCH | Đổi role (`{role: STUDENT\|TEACHER\|ADMIN}`) |
| `/admin/users/{id}/active` | PATCH | Bật/tắt trạng thái active |

#### 4.8.2. Quản lý khóa học

```
Admin ──▶ Admin Courses ──▶ CRUD khóa học
                │
                ├── GET /admin/courses?page=0&size=20
                ├── POST /admin/courses (Tạo mới)
                ├── PUT /admin/courses/{id} (Cập nhật)
                └── DELETE /admin/courses/{id} (Xóa)
```

| API | Method | Mô tả |
|---|---|---|
| `/admin/courses` | GET | Danh sách toàn bộ khóa học (phân trang) |
| `/admin/courses` | POST | Tạo khóa học mới |
| `/admin/courses/{id}` | PUT | Cập nhật khóa học |
| `/admin/courses/{id}` | DELETE | Xóa khóa học |

#### 4.8.3. Quản lý đăng ký (Enrollments)

```
Admin ──▶ Admin Enrollments ──▶ Duyệt/từ chối đăng ký học viên
                │
                ├── GET /admin/enrollments?status=&page=0&size=20
                ├── PATCH /admin/enrollments/{id}/approve (Duyệt)
                └── PATCH /admin/enrollments/{id}/revoke?note= (Từ chối)
```

| API | Method | Mô tả |
|---|---|---|
| `/admin/enrollments` | GET | Danh sách enrollment (lọc theo status, phân trang) |
| `/admin/enrollments/{id}/approve` | PATCH | Duyệt enrollment → status "active" |
| `/admin/enrollments/{id}/revoke` | PATCH | Từ chối/thu hồi → status "revoked" (kèm note) |

---

### 4.9. Luồng thông báo & lịch (Notifications & Calendar)

**Phân quyền**: Authenticated

```
Người dùng ──▶ Navbar ──▶ Xem thông báo (dropdown)
                   │
                   ├── GET /notifications
                   ├── GET /notifications/unread/count
                   ├── PUT /notifications/read-all
                   └── DELETE /notifications/{id}

Người dùng ──▶ Dashboard ──▶ Lịch cá nhân
                   │
                   ├── GET /calendar?year=&month=
                   ├── POST /calendar (Tạo sự kiện)
                   └── DELETE /calendar/{id} (Xóa sự kiện)
```

| API | Method | Mô tả |
|---|---|---|
| `/notifications` | GET | Tất cả thông báo |
| `/notifications/unread/count` | GET | Số thông báo chưa đọc |
| `/notifications/read-all` | PUT | Đánh dấu tất cả đã đọc |
| `/notifications/{id}` | DELETE | Xóa thông báo |
| `/calendar` | GET | Sự kiện lịch trong tháng |
| `/calendar` | POST | Tạo sự kiện lịch |
| `/calendar/{id}` | DELETE | Xóa sự kiện lịch |

---

### 4.10. Luồng nội dung khóa học (Course Content - Modules & Lessons)

**Phân quyền**: Xem modules/lessons công khai; quản lý cần TEACHER/ADMIN

```
Giáo viên ──▶ Quản lý khóa học ──▶ Tạo Module cho khóa học
                                          │
                               POST /teacher/courses/{courseId}/modules
                                          │
                                          ▼
                               Tạo Lessons trong Module
                                          │
                               POST /teacher/modules/{moduleId}/lessons
                                          │
                                          ▼
                               Cập nhật/Xóa Module hoặc Lesson
                                  │                    │
                     PUT/DELETE /teacher/modules/{id}  PUT/DELETE /teacher/lessons/{id}

Học viên ──▶ Xem chi tiết khóa học ──▶ Xem modules & lessons
                                          │
                               GET /courses/{courseId}/modules
                                          │
                                          ▼
                               Hoàn thành bài học
                                          │
                               POST /lessons/{lessonId}/complete
                                          │
                                          ▼
                               Tự động tính lại progress enrollment
```

> **Cấu trúc nội dung**: Course → Modules → Lessons. Mỗi khóa học có nhiều modules, mỗi module có nhiều lessons. Lessons có thể đánh dấu `is_free` để cho phép xem trước.

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/courses/{courseId}/modules` | GET | Công khai | Danh sách modules kèm lessons |
| `/teacher/courses/{courseId}/modules` | POST | TEACHER/ADMIN | Tạo module mới |
| `/teacher/modules/{moduleId}` | PUT | TEACHER/ADMIN | Cập nhật module |
| `/teacher/modules/{moduleId}` | DELETE | TEACHER/ADMIN | Xóa module |
| `/teacher/modules/{moduleId}/lessons` | POST | TEACHER/ADMIN | Tạo lesson mới |
| `/teacher/lessons/{lessonId}` | PUT | TEACHER/ADMIN | Cập nhật lesson |
| `/teacher/lessons/{lessonId}` | DELETE | TEACHER/ADMIN | Xóa lesson |
| `/lessons/{lessonId}/complete` | POST | Authenticated | Đánh dấu hoàn thành bài học (tự động tính progress) |

---

### 4.11. Luồng đánh giá khóa học (Reviews)

**Phân quyền**: Xem đánh giá công khai; viết đánh giá cần đăng nhập (yêu cầu ≥30% progress)

```
Học viên ──▶ Trang chi tiết khóa học ──▶ Xem đánh giá
                                              │
                                   GET /courses/{courseId}/reviews
                                              │
                                    (Nếu đã đăng ký & progress ≥ 30%)
                                              │
                                   POST /courses/{courseId}/reviews
                                              │
                                              ▼
                                   Đánh giá được tạo (rating 1-5 + comment)
                                              │
                                   Tự động cập nhật avg_rating & review_count trên courses
```

> **Quy tắc**: Học viên phải đạt ít nhất 30% tiến độ khóa học mới được đánh giá. Mỗi học viên chỉ đánh giá 1 lần/khóa. `avg_rating` và `review_count` được cache trên bảng `courses`.

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/courses/{courseId}/reviews` | GET | Công khai | Danh sách đánh giá của khóa học |
| `/courses/{courseId}/reviews` | POST | Authenticated | Tạo đánh giá (yêu cầu ≥30% progress) |
| `/courses/{courseId}/reviews/{reviewId}` | DELETE | Authenticated | Xóa đánh giá của chính mình |

---

### 4.12. Luồng thanh toán (Payments)

**Phân quyền**: Authenticated; callback công khai (webhook)

```
Học viên ──▶ Đăng ký khóa học có phí ──▶ Chọn phương thức thanh toán
                                                │
                                     POST /payments/initiate
                                     {courseId, method: "BANK_TRANSFER"|...}
                                                │
                                                ▼
                                     Payment tạo với status = "pending"
                                                │
                                     Chuyển hướng đến cổng thanh toán
                                                │
                                     Cổng thanh toán callback
                                                │
                                     POST /payments/callback
                                                │
                                                ▼
                                     Payment status → "completed"
                                     Enrollment status → "active"
                                     Gửi notification qua WebSocket
```

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/payments/initiate` | POST | Authenticated | Khởi tạo thanh toán (`{courseId, method}`) |
| `/payments/callback` | POST | Công khai (webhook) | Callback từ cổng thanh toán |
| `/payments/my` | GET | Authenticated | Lịch sử thanh toán của user |

---

### 4.13. Luồng quản lý dữ liệu chung (Master Data)

**Phân quyền**: Xem công khai; quản lý chỉ ADMIN

```
Admin ──▶ Quản lý Master Data ──▶ CRUD Danh mục & Cấp độ
                │
                ├── Categories (Danh mục khóa học)
                │     ├── GET /categories (Công khai)
                │     ├── POST /admin/master-data/categories
                │     ├── PUT /admin/master-data/categories/{id}
                │     └── DELETE /admin/master-data/categories/{id}
                │
                └── Levels (Cấp độ khóa học)
                      ├── GET /levels (Công khai)
                      ├── POST /admin/master-data/levels
                      ├── PUT /admin/master-data/levels/{id}
                      └── DELETE /admin/master-data/levels/{id}
```

| API | Method | Phân quyền | Mô tả |
|---|---|---|---|
| `/categories` | GET | Công khai | Danh sách tất cả danh mục |
| `/categories/{id}` | GET | Công khai | Chi tiết danh mục |
| `/levels` | GET | Công khai | Danh sách tất cả cấp độ |
| `/levels/{id}` | GET | Công khai | Chi tiết cấp độ |
| `/admin/master-data/categories` | POST | ADMIN | Tạo danh mục |
| `/admin/master-data/categories/{id}` | PUT | ADMIN | Cập nhật danh mục |
| `/admin/master-data/categories/{id}` | DELETE | ADMIN | Xóa danh mục |
| `/admin/master-data/levels` | POST | ADMIN | Tạo cấp độ |
| `/admin/master-data/levels/{id}` | PUT | ADMIN | Cập nhật cấp độ |
| `/admin/master-data/levels/{id}` | DELETE | ADMIN | Xóa cấp độ |

---

## 5. Danh sách API chi tiết

### Tổng hợp tất cả API endpoints

| # | Method | Endpoint | Phân quyền | Mô tả |
|---|---|---|---|---|
| 1 | POST | `/auth/login` | Công khai | Đăng nhập |
| 2 | POST | `/auth/guest` | Công khai | Đăng nhập khách |
| 3 | POST | `/auth/logout` | Authenticated | Đăng xuất |
| 4 | POST | `/auth/forgot-password` | Công khai | Yêu cầu reset password |
| 5 | POST | `/auth/reset-password` | Công khai | Đặt lại mật khẩu |
| 6 | GET | `/courses` | Công khai | Danh sách khóa học (published) |
| 7 | GET | `/courses/{id}` | Công khai | Chi tiết khóa học |
| 8 | GET | `/courses/enrolled` | Authenticated | Khóa học đã đăng ký |
| 9 | GET | `/courses/recent` | Authenticated | Khóa học gần đây |
| 10 | GET | `/courses/dashboard` | Authenticated | Thống kê enrollment |
| 11 | POST | `/courses/{id}/enroll` | STUDENT+ | Đăng ký khóa học |
| 12 | PATCH | `/courses/{id}/enrollment` | STUDENT+ | Cập nhật tiến độ |
| 13 | GET | `/users/me` | Authenticated | Xem profile |
| 14 | PUT | `/users/me` | Authenticated | Cập nhật profile |
| 15 | POST | `/users/me/password` | Authenticated | Đổi mật khẩu |
| 16 | POST | `/users/me/avatar` | Authenticated | Upload avatar |
| 17 | GET | `/notifications` | Authenticated | Danh sách thông báo |
| 18 | GET | `/notifications/unread/count` | Authenticated | Đếm chưa đọc |
| 19 | PUT | `/notifications/read-all` | Authenticated | Đánh dấu đã đọc |
| 20 | DELETE | `/notifications/{id}` | Authenticated | Xóa thông báo |
| 21 | GET | `/timeline` | Authenticated | Sự kiện sắp tới |
| 22 | GET | `/calendar` | Authenticated | Lịch cá nhân |
| 23 | POST | `/calendar` | Authenticated | Tạo sự kiện lịch |
| 24 | DELETE | `/calendar/{id}` | Authenticated | Xóa sự kiện lịch |
| 25 | GET | `/teacher/dashboard` | TEACHER/ADMIN | Thống kê giáo viên |
| 26 | GET | `/teacher/courses` | TEACHER/ADMIN | Khóa học được giao |
| 27 | GET | `/teacher/enrollments` | TEACHER/ADMIN | Enrollment trong khóa học |
| 28 | PATCH | `/teacher/enrollments/{id}` | TEACHER/ADMIN | Cập nhật enrollment |
| 29 | GET | `/admin/users` | ADMIN | Quản lý users |
| 30 | GET | `/admin/users/{id}` | ADMIN | Chi tiết user |
| 31 | PATCH | `/admin/users/{id}/role` | ADMIN | Đổi role |
| 32 | PATCH | `/admin/users/{id}/active` | ADMIN | Bật/tắt active |
| 33 | GET | `/admin/courses` | ADMIN | Quản lý khóa học |
| 34 | POST | `/admin/courses` | ADMIN | Tạo khóa học |
| 35 | PUT | `/admin/courses/{id}` | ADMIN | Cập nhật khóa học |
| 36 | DELETE | `/admin/courses/{id}` | ADMIN | Xóa khóa học |
| 37 | GET | `/admin/enrollments` | ADMIN | Quản lý enrollment |
| 38 | PATCH | `/admin/enrollments/{id}/approve` | ADMIN | Duyệt enrollment |
| 39 | PATCH | `/admin/enrollments/{id}/revoke` | ADMIN | Từ chối/thu hồi enrollment |
| | | **Master Data** | | |
| 40 | GET | `/categories` | Công khai | Danh sách danh mục |
| 41 | GET | `/categories/{id}` | Công khai | Chi tiết danh mục |
| 42 | GET | `/levels` | Công khai | Danh sách cấp độ |
| 43 | GET | `/levels/{id}` | Công khai | Chi tiết cấp độ |
| 44 | POST | `/admin/master-data/categories` | ADMIN | Tạo danh mục |
| 45 | PUT | `/admin/master-data/categories/{id}` | ADMIN | Cập nhật danh mục |
| 46 | DELETE | `/admin/master-data/categories/{id}` | ADMIN | Xóa danh mục |
| 47 | POST | `/admin/master-data/levels` | ADMIN | Tạo cấp độ |
| 48 | PUT | `/admin/master-data/levels/{id}` | ADMIN | Cập nhật cấp độ |
| 49 | DELETE | `/admin/master-data/levels/{id}` | ADMIN | Xóa cấp độ |
| | | **Course Content** | | |
| 50 | GET | `/courses/{courseId}/modules` | Công khai | Danh sách modules kèm lessons |
| 51 | POST | `/teacher/courses/{courseId}/modules` | TEACHER/ADMIN | Tạo module |
| 52 | PUT | `/teacher/modules/{moduleId}` | TEACHER/ADMIN | Cập nhật module |
| 53 | DELETE | `/teacher/modules/{moduleId}` | TEACHER/ADMIN | Xóa module |
| 54 | POST | `/teacher/modules/{moduleId}/lessons` | TEACHER/ADMIN | Tạo lesson |
| 55 | PUT | `/teacher/lessons/{lessonId}` | TEACHER/ADMIN | Cập nhật lesson |
| 56 | DELETE | `/teacher/lessons/{lessonId}` | TEACHER/ADMIN | Xóa lesson |
| 57 | POST | `/lessons/{lessonId}/complete` | Authenticated | Đánh dấu hoàn thành bài học |
| | | **Reviews** | | |
| 58 | GET | `/courses/{courseId}/reviews` | Công khai | Danh sách đánh giá |
| 59 | POST | `/courses/{courseId}/reviews` | Authenticated | Tạo đánh giá (≥30% progress) |
| 60 | DELETE | `/courses/{courseId}/reviews/{reviewId}` | Authenticated | Xóa đánh giá của mình |
| | | **Payments** | | |
| 61 | POST | `/payments/initiate` | Authenticated | Khởi tạo thanh toán |
| 62 | POST | `/payments/callback` | Công khai (webhook) | Callback cổng thanh toán |
| 63 | GET | `/payments/my` | Authenticated | Lịch sử thanh toán |
| | | **WebSocket** | | |
| 64 | — | `/ws` (STOMP + SockJS) | Công khai | WebSocket endpoint |
| 65 | — | `/user/{userId}/queue/notifications` | Authenticated | Real-time notifications |

> Tất cả API đều có prefix `/api/v1`. Ví dụ: `GET /api/v1/courses`

---

## 6. Cơ sở dữ liệu

### Entity Relationship Diagram

```
┌──────────────┐       ┌─────────────────┐       ┌──────────────────┐
│    users     │       │   enrollments   │       │     courses      │
├──────────────┤       ├─────────────────┤       ├──────────────────┤
│ id (PK)      │◀──┐   │ id (PK)         │   ┌──▶│ id (PK)          │
│ username     │   ├───│ user_id (FK)    │   │   │ external_id      │
│ email        │   │   │ course_id (FK)  │───┘   │ name             │
│ password     │   │   │ status          │       │ category         │
│ first_name   │   │   │ progress        │       │ level            │
│ last_name    │   │   │ enrolled_at     │       │ description      │
│ avatar_url   │   │   │ last_accessed   │       │ image_url        │
│ role         │   │   │ request_date    │       │ teacher_id (FK)  │──┐
│ is_active    │   │   │ approved_at     │       │ category_id (FK) │──┼─▶ categories
│ is_guest     │   │   │ approved_by(FK) │───┐   │ level_id (FK)    │──┼─▶ levels
│ created_at   │   │   │ teacher_note    │   │   │ is_published     │  │
│ updated_at   │   │   │ starred         │   │   │ is_free          │  │
               │   │   │ hidden          │   │   │ price            │  │
               │   │   │ expiry_date     │   │   │ avg_rating       │  │
               │   │   └─────────────────┘   │   │ review_count     │  │
               │   │                         │   │ created_at       │  │
               │   │   (approved_by → users.id)   └──────┬───────────┘  │
               │   │                                     │              │
       ┌───────┘   │                     (teacher_id → users.id)────────┘
       │           │
       │    ┌──────┴──────────┐   ┌────────────────────┐   ┌────────────────────┐
       │    │  notifications  │   │  calendar_events   │   │  timeline_events   │
       │    ├─────────────────┤   ├────────────────────┤   ├────────────────────┤
       ├───▶│ id (PK)         │   │ id (PK)            │   │ id (PK)            │
       ├───▶│ user_id (FK)    │   │ user_id (FK)    ◀──┤───│ user_id (FK)       │
       │    │ message         │   │ title              │   │ course_id (FK)     │
       │    │ link            │   │ description        │   │ title              │
       │    │ is_read         │   │ event_date         │   │ type               │
       │    │ type            │   │ created_at         │   │ due_at             │
       │    │ created_at      │   └────────────────────┘   │ completed_at       │
       │    └─────────────────┘                            │ created_at         │
       │                                                   └────────────────────┘
       │    ┌───────────────────────┐
       └───▶│ password_reset_tokens │
            ├───────────────────────┤
            │ id (PK)               │
            │ user_id (FK)          │
            │ token (UNIQUE)        │
            │ expiry_at             │
            │ created_at            │
            └───────────────────────┘

┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  categories  │    │     levels       │    │ course_teachers  │
├──────────────┤    ├──────────────────┤    ├──────────────────┤
│ id (PK)      │    │ id (PK)          │    │ id (PK)          │
│ name         │    │ name             │    │ course_id (FK)   │───▶ courses
│ slug         │    │ slug             │    │ teacher_id (FK)  │───▶ users
│ description  │    │ sort_order       │    │ role             │ (PRIMARY, ASSISTANT)
│ sort_order   │    │ created_at       │    │ created_at       │
│ created_at   │    └──────────────────┘    └──────────────────┘
└──────────────┘

┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ course_modules   │    │    lessons       │    │ lesson_progress  │
├──────────────────┤    ├──────────────────┤    ├──────────────────┤
│ id (PK)          │    │ id (PK)          │    │ id (PK)          │
│ course_id (FK)   │──▶ │ module_id (FK)   │──▶ │ user_id (FK)     │───▶ users
│ title            │    │ title            │    │ lesson_id (FK)   │───▶ lessons
│ description      │    │ content          │    │ status           │
│ sort_order       │    │ type             │    │ completed_at     │
│ created_at       │    │ video_url        │    │ created_at       │
│ updated_at       │    │ duration         │    └──────────────────┘
└──────────────────┘    │ sort_order       │
       ▲                │ is_free          │    ┌──────────────────┐
       │                │ created_at       │    │ course_reviews   │
  Course → Modules      │ updated_at       │    ├──────────────────┤
  → Lessons             └──────────────────┘    │ id (PK)          │
                                                │ course_id (FK)   │───▶ courses
┌──────────────────┐                            │ user_id (FK)     │───▶ users
│    payments      │                            │ rating           │ (1-5)
├──────────────────┤                            │ comment          │
│ id (PK)          │                            │ created_at       │
│ user_id (FK)     │───▶ users                  │ updated_at       │
│ course_id (FK)   │───▶ courses                └──────────────────┘
│ enrollment_id(FK)│───▶ enrollments
│ amount           │
│ currency         │
│ payment_method   │
│ transaction_id   │
│ status           │
│ paid_at          │
│ created_at       │
│ updated_at       │
└──────────────────┘
```

### Bảng chi tiết

#### `users`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| username | VARCHAR(100) | UNIQUE, NOT NULL | Tên đăng nhập |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email |
| password | VARCHAR(255) | NOT NULL | Mật khẩu (BCrypt) |
| first_name | VARCHAR(100) | | Họ |
| last_name | VARCHAR(100) | | Tên |
| avatar_url | VARCHAR(512) | | URL ảnh đại diện (MinIO) |
| role | VARCHAR(20) | DEFAULT 'STUDENT' | STUDENT, TEACHER, ADMIN |
| is_active | BOOLEAN | DEFAULT TRUE | Trạng thái tài khoản |
| is_guest | BOOLEAN | DEFAULT FALSE | Tài khoản khách |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

#### `courses`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| external_id | INTEGER | UNIQUE | ID từ hệ thống bên ngoài |
| name | VARCHAR(500) | NOT NULL | Tên khóa học |
| category | VARCHAR(50) | | Danh mục (English, Math...) — legacy |
| level | VARCHAR(50) | | Cấp độ — legacy |
| description | TEXT | | Mô tả |
| image_url | VARCHAR(512) | | Ảnh khóa học |
| teacher_id | BIGINT | FK → users.id | Giáo viên phụ trách (legacy, xem course_teachers) |
| category_id | BIGINT | FK → categories.id | Danh mục khóa học (mới) |
| level_id | BIGINT | FK → levels.id | Cấp độ khóa học (mới) |
| is_published | BOOLEAN | DEFAULT TRUE | Trạng thái công khai |
| is_free | BOOLEAN | DEFAULT TRUE | Miễn phí hay có phí |
| price | DECIMAL(12,2) | | Giá khóa học (nếu có phí) |
| avg_rating | DECIMAL(3,2) | DEFAULT 0 | Điểm đánh giá trung bình (cache) |
| review_count | INTEGER | DEFAULT 0 | Số lượng đánh giá (cache) |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `enrollments`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id, NOT NULL | Học viên |
| course_id | BIGINT | FK → courses.id, NOT NULL | Khóa học |
| status | VARCHAR(20) | DEFAULT 'pending' | pending, active, inprogress, completed, revoked |
| progress | INTEGER | DEFAULT 0 | Tiến độ (0-100%) |
| enrolled_at | TIMESTAMP | DEFAULT NOW() | Ngày đăng ký |
| last_accessed | TIMESTAMP | | Lần truy cập cuối |
| request_date | TIMESTAMP | | Ngày yêu cầu |
| approved_at | TIMESTAMP | | Ngày duyệt |
| approved_by | BIGINT | FK → users.id | Admin đã duyệt |
| teacher_note | TEXT | | Ghi chú của giáo viên |
| starred | BOOLEAN | DEFAULT FALSE | Đánh dấu yêu thích |
| hidden | BOOLEAN | DEFAULT FALSE | Ẩn khỏi danh sách |
| expiry_date | TIMESTAMP | | Ngày hết hạn |
| | | UNIQUE(user_id, course_id) | Mỗi user chỉ đăng ký 1 lần/khóa |

#### `notifications`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id, NOT NULL | Người nhận |
| message | TEXT | NOT NULL | Nội dung thông báo |
| link | VARCHAR(500) | | Link liên kết |
| is_read | BOOLEAN | DEFAULT FALSE | Đã đọc chưa |
| type | VARCHAR(50) | DEFAULT 'GENERAL' | Loại thông báo (GENERAL, ENROLLMENT, PAYMENT, etc.) |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `calendar_events`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id, NOT NULL | Chủ sự kiện |
| title | VARCHAR(300) | NOT NULL | Tiêu đề |
| description | TEXT | | Mô tả |
| event_date | DATE | NOT NULL | Ngày sự kiện |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `timeline_events`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id | Người dùng |
| course_id | BIGINT | FK → courses.id | Khóa học liên quan |
| title | VARCHAR(500) | NOT NULL | Tiêu đề |
| type | VARCHAR(50) | | Loại sự kiện |
| due_at | TIMESTAMP | | Hạn chót |
| completed_at | TIMESTAMP | | Ngày hoàn thành |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `password_reset_tokens`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id, NOT NULL | Người yêu cầu |
| token | VARCHAR | UNIQUE | Token reset |
| expiry_at | TIMESTAMP | | Hạn token |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `categories`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| name | VARCHAR(100) | NOT NULL | Tên danh mục |
| slug | VARCHAR(100) | UNIQUE | Slug URL-friendly |
| description | TEXT | | Mô tả |
| sort_order | INTEGER | DEFAULT 0 | Thứ tự sắp xếp |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `levels`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| name | VARCHAR(100) | NOT NULL | Tên cấp độ |
| slug | VARCHAR(100) | UNIQUE | Slug URL-friendly |
| sort_order | INTEGER | DEFAULT 0 | Thứ tự sắp xếp |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `course_teachers`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| course_id | BIGINT | FK → courses.id, NOT NULL | Khóa học |
| teacher_id | BIGINT | FK → users.id, NOT NULL | Giáo viên |
| role | VARCHAR(20) | DEFAULT 'PRIMARY' | Vai trò: PRIMARY, ASSISTANT |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `course_modules`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| course_id | BIGINT | FK → courses.id, NOT NULL | Khóa học |
| title | VARCHAR(500) | NOT NULL | Tiêu đề module |
| description | TEXT | | Mô tả |
| sort_order | INTEGER | DEFAULT 0 | Thứ tự sắp xếp |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

#### `lessons`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| module_id | BIGINT | FK → course_modules.id, NOT NULL | Module chứa bài học |
| title | VARCHAR(500) | NOT NULL | Tiêu đề bài học |
| content | TEXT | | Nội dung bài học |
| type | VARCHAR(50) | | Loại: VIDEO, TEXT, QUIZ, etc. |
| video_url | VARCHAR(512) | | URL video bài học |
| duration | INTEGER | | Thời lượng (phút) |
| sort_order | INTEGER | DEFAULT 0 | Thứ tự sắp xếp |
| is_free | BOOLEAN | DEFAULT FALSE | Cho phép xem miễn phí (preview) |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

#### `lesson_progress`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id, NOT NULL | Học viên |
| lesson_id | BIGINT | FK → lessons.id, NOT NULL | Bài học |
| status | VARCHAR(20) | DEFAULT 'NOT_STARTED' | Trạng thái: NOT_STARTED, COMPLETED |
| completed_at | TIMESTAMP | | Ngày hoàn thành |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |

#### `payments`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| user_id | BIGINT | FK → users.id, NOT NULL | Người thanh toán |
| course_id | BIGINT | FK → courses.id, NOT NULL | Khóa học |
| enrollment_id | BIGINT | FK → enrollments.id | Enrollment liên quan |
| amount | DECIMAL(12,2) | NOT NULL | Số tiền |
| currency | VARCHAR(10) | DEFAULT 'VND' | Đơn vị tiền tệ |
| payment_method | VARCHAR(50) | | Phương thức: BANK_TRANSFER, MOMO, etc. |
| transaction_id | VARCHAR(255) | | Mã giao dịch từ cổng thanh toán |
| status | VARCHAR(20) | DEFAULT 'pending' | pending, completed, failed, refunded |
| paid_at | TIMESTAMP | | Ngày thanh toán thành công |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |

#### `course_reviews`
| Cột | Kiểu | Ràng buộc | Mô tả |
|---|---|---|---|
| id | BIGSERIAL | PK | ID tự tăng |
| course_id | BIGINT | FK → courses.id, NOT NULL | Khóa học |
| user_id | BIGINT | FK → users.id, NOT NULL | Người đánh giá |
| rating | INTEGER | NOT NULL, CHECK(1-5) | Điểm đánh giá (1-5 sao) |
| comment | TEXT | | Nhận xét |
| created_at | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updated_at | TIMESTAMP | DEFAULT NOW() | Ngày cập nhật |
| | | UNIQUE(course_id, user_id) | Mỗi user chỉ đánh giá 1 lần/khóa |

---

## 7. Frontend - Danh sách trang

| # | Trang | URL | File TS | Quyền | Guard |
|---|---|---|---|---|---|
| 1 | Trang chủ | `/` | `src/home/home.ts` | Công khai | — |
| 2 | Đăng nhập | `/pages/login/` | `pages/login/login.ts` | Công khai | — |
| 3 | Quên mật khẩu | `/pages/forgot-password/` | `pages/forgot-password/forgot-password.ts` | Công khai | — |
| 4 | Chi tiết khóa học | `/pages/course/?id=X` | `pages/course/course.ts` | Công khai | — |
| 5 | Hồ sơ | `/pages/profile/` | `pages/profile/profile.ts` | Authenticated | `isLoggedIn()` |
| 6 | Dashboard | `/pages/my/dashboard/` | `pages/my/dashboard/dashboard.ts` | STUDENT+ | — |
| 7 | Khóa học của tôi | `/pages/my/courses/` | `pages/my/courses/courses.ts` | STUDENT+ | — |
| 8 | GV - Dashboard | `/pages/teacher/dashboard/` | `pages/teacher/dashboard/dashboard.ts` | TEACHER/ADMIN | `requireTeacher()` |
| 9 | GV - Học viên | `/pages/teacher/students/` | `pages/teacher/students/students.ts` | TEACHER/ADMIN | `requireTeacher()` |
| 10 | QT - Dashboard | `/pages/admin/` | `pages/admin/admin.ts` | ADMIN | `requireAdmin()` |
| 11 | QT - Người dùng | `/pages/admin/users/` | `pages/admin/users/users.ts` | ADMIN | `requireAdmin()` |
| 12 | QT - Khóa học | `/pages/admin/courses/` | `pages/admin/courses/courses.ts` | ADMIN | `requireAdmin()` |
| 13 | QT - Đăng ký | `/pages/admin/enrollments/` | `pages/admin/enrollments/enrollments.ts` | ADMIN | `requireAdmin()` |
| 14 | Quản lý | `/pages/manage/` | `pages/manage/manage.ts` | ADMIN | `requireAdmin()` |

### Shared Components (Tái sử dụng)

| Component | File | Mô tả |
|---|---|---|
| Navbar | `src/shared/js/inject-navbar.ts` | Inject navbar responsive + offcanvas sidebar |
| Navbar Behavior | `src/shared/js/navbar.ts` | Auth state, user initials, role menu, logout, notifications |
| Footer | `src/shared/js/footer.ts` | Inject footer (guest/logged-in state) |
| i18n | `src/shared/js/i18n.ts` | Đa ngôn ngữ (vi/en) |
| API Client | `src/shared/js/api.ts` | Wrapper cho fetch API (apiGet, apiPost, apiPut, apiPatch, apiDelete) |
| Toast | `src/shared/js/toast.ts` | Thông báo toast |
| Auth Utils | `src/shared/js/auth.ts` | isLoggedIn(), getToken(), getUser(), requireAdmin(), requireTeacher() |
