# Tài Liệu Kỹ Thuật — Sunshine School Online

> **Phiên bản**: 1.0.0 | **Cập nhật lần cuối**: 2025

---

## Mục Lục

1. [Tổng Quan Hệ Thống](#1-tổng-quan-hệ-thống)
2. [Kiến Trúc Hệ Thống](#2-kiến-trúc-hệ-thống)
3. [Công Nghệ Sử Dụng](#3-công-nghệ-sử-dụng)
4. [Cấu Trúc Dự Án](#4-cấu-trúc-dự-án)
5. [Xác Thực & Bảo Mật](#5-xác-thực--bảo-mật)
6. [Danh Sách API Đầy Đủ](#6-danh-sách-api-đầy-đủ)
7. [Luồng Hoạt Động Chức Năng](#7-luồng-hoạt-động-chức-năng)
8. [Tích Hợp Moodle LMS](#8-tích-hợp-moodle-lms)
9. [Cơ Sở Dữ Liệu](#9-cơ-sở-dữ-liệu)
10. [WebSocket & Thông Báo Thời Gian Thực](#10-websocket--thông-báo-thời-gian-thực)
11. [Lưu Trữ Tệp (MinIO)](#11-lưu-trữ-tệp-minio)
12. [Cấu Hình Triển Khai](#12-cấu-hình-triển-khai)

---

## 1. Tổng Quan Hệ Thống

**Sunshine School Online** là nền tảng học trực tuyến (E-Learning) tích hợp giữa hệ thống quản lý tùy chỉnh (Custom LMS) và Moodle LMS. Người dùng đăng nhập một lần (SSO) trên frontend và có thể truy cập trực tiếp vào nội dung học tập trong Moodle mà không cần đăng nhập lại.

### Đối Tượng Người Dùng

| Vai trò       | Mô tả                                                                 |
|---------------|----------------------------------------------------------------------|
| **GUEST**     | Người dùng chưa đăng nhập, chỉ xem danh sách khóa học công khai     |
| **STUDENT**   | Học viên đã đăng ký, tham gia khóa học, làm bài tập, xem điểm       |
| **TEACHER**   | Giảng viên được phân công khóa học, quản lý học viên và nội dung     |
| **ADMIN**     | Quản trị viên toàn hệ thống, quản lý người dùng, khóa học, đăng ký  |

---

## 2. Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT BROWSER                           │
│   Sunshine Frontend (Vite + TypeScript + Bootstrap 5)           │
│   Port: 5173 (dev) / Static files (prod)                        │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP/REST + WebSocket (STOMP)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NGINX REVERSE PROXY                          │
│   Port 80/443 — Routes /api/* → Backend, /* → Frontend         │
└──────────┬────────────────────────────────────────┬────────────┘
           │                                        │
           ▼                                        ▼
┌──────────────────────┐                 ┌──────────────────────┐
│   Spring Boot API    │                 │    Moodle LMS        │
│   Port: 4000         │ ◄──── HTTP ───► │    Port: 8088        │
│   (sso-backend)      │  Moodle REST    │    (PHP + MariaDB)   │
└──────────┬───────────┘                 └──────────────────────┘
           │
   ┌───────┼────────────────┐
   ▼       ▼                ▼
┌──────┐ ┌──────┐      ┌──────────┐
│ PG   │ │Redis │      │  MinIO   │
│ :5432│ │:6379 │      │  :9000   │
│(Data)│ │(JWT  │      │(File     │
│      │ │Black │      │ Storage) │
└──────┘ │list) │      └──────────┘
         └──────┘
```

### Luồng SSO (Đăng Nhập Một Lần)

```
Frontend → Login API → Nhận JWT → Gọi /api/v1/moodle/launch
→ Backend tạo user Moodle (nếu chưa có) → Tạo SSO key → URL redirect
→ Moodle xác thực key → Người dùng vào Moodle đã đăng nhập
```

---

## 3. Công Nghệ Sử Dụng

### Frontend

| Thành phần     | Công nghệ             | Phiên bản |
|----------------|-----------------------|-----------|
| Build Tool     | Vite                  | 5.x       |
| Language       | TypeScript            | 5.x       |
| UI Framework   | Bootstrap             | 5.3       |
| Icons          | Font Awesome          | 6.5       |
| HTTP Client    | Native Fetch API      | —         |
| Realtime       | SockJS + STOMP.js     | —         |
| i18n           | Custom (JSON files)   | —         |

### Backend

| Thành phần     | Công nghệ             | Phiên bản |
|----------------|-----------------------|-----------|
| Framework      | Spring Boot           | 4.0.3     |
| Language       | Java                  | 17        |
| Build Tool     | Maven                 | 3.x       |
| Database       | PostgreSQL            | 15        |
| Migration      | Flyway                | —         |
| Cache/Blacklist| Redis                 | 7          |
| JWT            | JJWT (HS256, 24h)     | 0.12.6    |
| File Storage   | MinIO (S3-compatible) | —         |
| WebSocket      | STOMP over SockJS     | —         |
| HTTP Client    | JDK HttpClient        | —         |

---

## 4. Cấu Trúc Dự Án

```
EnglishEdu/
├── sunshine-rebuild/           # Frontend (Vite + TypeScript)
│   ├── index.html              # Trang chủ (landing page)
│   ├── vite.config.js          # Cấu hình Vite (multi-page)
│   ├── src/
│   │   ├── home/               # Entry TS cho trang chủ
│   │   └── shared/
│   │       ├── js/             # Shared utilities
│   │       │   ├── api.ts          # HTTP client wrapper
│   │       │   ├── auth.ts         # Auth helpers (token, role)
│   │       │   ├── inject-navbar.ts# Inject shared navbar HTML
│   │       │   ├── navbar.ts       # Navbar behavior + auth state
│   │       │   ├── footer.ts       # Inject shared footer HTML
│   │       │   ├── i18n.ts         # Internationalization
│   │       │   └── types.ts        # TypeScript interfaces
│   │       └── styles/
│   │           ├── shared.css      # Imports all partials
│   │           ├── base/           # _variables.css, _reset.css, _typography.css
│   │           └── layout/         # _navbar.css, _footer.css
│   └── pages/
│       ├── login/              # Đăng nhập / Đăng ký
│       ├── forgot-password/    # Quên mật khẩu / Đặt lại mật khẩu
│       ├── course/             # Chi tiết khóa học công khai
│       ├── my/
│       │   ├── dashboard/      # Trang chủ học viên
│       │   ├── courses/        # Khóa học của tôi
│       │   └── grades/         # Bảng điểm
│       ├── learn/              # Trang học (Moodle iframe)
│       ├── profile/            # Hồ sơ người dùng
│       ├── manage/             # Quản lý đăng ký (Admin)
│       ├── admin/              # Bảng điều khiển Admin
│       └── teacher/            # Bảng điều khiển Giảng viên
│
├── EnglishEdu_BE/              # Backend (Spring Boot)
│   └── src/main/java/com/sso/
│       ├── controller/         # REST controllers
│       ├── service/            # Business logic
│       ├── repository/         # JPA repositories
│       ├── entity/             # JPA entities
│       ├── dto/                # Request/Response DTOs
│       ├── security/           # JWT, filters, config
│       └── integration/        # Moodle HTTP client
│
├── moddle-lms/                 # Moodle LMS (Docker)
├── nginx/                      # Nginx config
└── docker-compose.prod.yml     # Production compose
```

---

## 5. Xác Thực & Bảo Mật

### Cơ Chế JWT

- **Thuật toán**: HS256
- **Thời hạn**: 24 giờ
- **Lưu trữ phía client**: `localStorage` (key: `authToken`)
- **Blacklist**: Token bị thu hồi lưu trong Redis (khi logout)
- **Header**: `Authorization: Bearer <token>`

### Phân Quyền

| Role      | Mức độ truy cập                                                    |
|-----------|--------------------------------------------------------------------|
| PUBLIC    | Không cần token (danh sách khóa học, chi tiết, categories)        |
| STUDENT   | Cần token, role = STUDENT hoặc ADMIN                              |
| TEACHER   | Cần token, role = TEACHER hoặc ADMIN                              |
| ADMIN     | Cần token, role = ADMIN                                           |

### Security Filter Chain

```
Request → JwtAuthFilter (extract + validate JWT) → SecurityContext
→ Spring Security authorization (role check) → Controller
```

---

## 6. Danh Sách API Đầy Đủ

**Base URL**: `http://localhost:4000/api/v1`

---

### 6.1 Auth — `/api/v1/auth`

| Method | Endpoint              | Auth     | Mô tả                                  |
|--------|-----------------------|----------|----------------------------------------|
| POST   | `/register`           | PUBLIC   | Đăng ký tài khoản mới                 |
| POST   | `/login`              | PUBLIC   | Đăng nhập, trả về JWT                 |
| POST   | `/guest`              | PUBLIC   | Tạo phiên khách (guest token)         |
| POST   | `/forgot-password`    | PUBLIC   | Gửi email đặt lại mật khẩu           |
| POST   | `/reset-password`     | PUBLIC   | Đặt lại mật khẩu bằng token email    |
| POST   | `/logout`             | AUTH     | Thu hồi token (thêm vào Redis blacklist) |

**Request/Response tiêu biểu:**

```json
// POST /auth/login
{
  "username": "student@example.com",
  "password": "password123"
}

// Response 200
{
  "token": "eyJhbGci...",
  "user": { "id": 1, "username": "student", "role": "STUDENT", "email": "..." }
}
```

---

### 6.2 User — `/api/v1/users`

| Method | Endpoint              | Auth     | Mô tả                                  |
|--------|-----------------------|----------|----------------------------------------|
| GET    | `/me`                 | AUTH     | Lấy thông tin hồ sơ người dùng hiện tại |
| PUT    | `/me`                 | AUTH     | Cập nhật thông tin hồ sơ              |
| POST   | `/me/password`        | AUTH     | Đổi mật khẩu                          |
| POST   | `/me/avatar`          | AUTH     | Upload ảnh đại diện (multipart/form-data) |

---

### 6.3 Course — `/api/v1/courses`

| Method | Endpoint                   | Auth     | Mô tả                                     |
|--------|----------------------------|----------|--------------------------------------------|
| GET    | `/`                        | PUBLIC   | Danh sách khóa học (params: category, page, size) |
| GET    | `/{id}`                    | PUBLIC   | Chi tiết một khóa học                     |
| GET    | `/enrolled`                | STUDENT  | Danh sách khóa học đã đăng ký             |
| GET    | `/assigned`                | TEACHER  | Danh sách khóa học được phân công         |
| GET    | `/recent`                  | STUDENT  | Khóa học truy cập gần đây                |
| GET    | `/dashboard-stats`         | STUDENT  | Thống kê tổng quan cho dashboard         |
| POST   | `/enroll/{id}`             | STUDENT  | Đăng ký khóa học (→ trạng thái PENDING)  |
| PATCH  | `/enrollment/{id}`         | STUDENT  | Hủy đăng ký khóa học                     |

---

### 6.4 Review — `/api/v1/courses/{courseId}/reviews`

| Method | Endpoint    | Auth    | Mô tả                           |
|--------|-------------|---------|----------------------------------|
| GET    | `/`         | PUBLIC  | Danh sách đánh giá của khóa học |
| POST   | `/`         | STUDENT | Tạo đánh giá mới               |
| DELETE | `/{id}`     | STUDENT | Xóa đánh giá của mình          |

---

### 6.5 Payment — `/api/v1/payments`

| Method | Endpoint      | Auth    | Mô tả                                |
|--------|---------------|---------|---------------------------------------|
| POST   | `/initiate`   | STUDENT | Khởi tạo giao dịch thanh toán        |
| POST   | `/callback`   | PUBLIC  | Nhận callback từ cổng thanh toán     |
| GET    | `/history`    | STUDENT | Lịch sử giao dịch của người dùng     |

---

### 6.6 Notification — `/api/v1/notifications`

| Method | Endpoint        | Auth | Mô tả                                |
|--------|-----------------|------|---------------------------------------|
| GET    | `/`             | AUTH | Danh sách thông báo                  |
| GET    | `/unread-count` | AUTH | Số lượng thông báo chưa đọc          |
| PUT    | `/read-all`     | AUTH | Đánh dấu tất cả là đã đọc           |
| DELETE | `/{id}`         | AUTH | Xóa một thông báo                   |

---

### 6.7 Category — `/api/v1/categories`

| Method | Endpoint  | Auth   | Mô tả              |
|--------|-----------|--------|--------------------|
| GET    | `/`       | PUBLIC | Danh sách danh mục |
| GET    | `/{id}`   | PUBLIC | Chi tiết danh mục  |

---

### 6.8 Level — `/api/v1/levels`

| Method | Endpoint  | Auth   | Mô tả                 |
|--------|-----------|--------|-----------------------|
| GET    | `/`       | PUBLIC | Danh sách cấp độ học  |
| GET    | `/{id}`   | PUBLIC | Chi tiết cấp độ học   |

---

### 6.9 Teacher — `/api/v1/teacher`

| Method | Endpoint            | Auth    | Mô tả                                     |
|--------|---------------------|---------|-------------------------------------------|
| GET    | `/dashboard`        | TEACHER | Thống kê dashboard giảng viên             |
| GET    | `/courses`          | TEACHER | Danh sách khóa học được phân công (phân trang) |
| GET    | `/enrollments`      | TEACHER | Danh sách học viên (filter: courseId, status) |
| PATCH  | `/enrollments/{id}` | TEACHER | Cập nhật trạng thái học viên              |

---

### 6.10 Admin — Người Dùng — `/api/v1/admin/users`

| Method | Endpoint         | Auth  | Mô tả                            |
|--------|------------------|-------|----------------------------------|
| GET    | `/`              | ADMIN | Danh sách tất cả người dùng     |
| GET    | `/{id}`          | ADMIN | Chi tiết người dùng              |
| POST   | `/`              | ADMIN | Tạo tài khoản mới               |
| PATCH  | `/{id}/role`     | ADMIN | Thay đổi vai trò người dùng     |
| PATCH  | `/{id}/active`   | ADMIN | Kích hoạt / vô hiệu hóa tài khoản |

---

### 6.11 Admin — Khóa Học — `/api/v1/admin/courses`

| Method | Endpoint         | Auth  | Mô tả                                    |
|--------|------------------|-------|------------------------------------------|
| GET    | `/`              | ADMIN | Danh sách khóa học                       |
| POST   | `/`              | ADMIN | Tạo khóa học mới (tự động sync Moodle)  |
| PUT    | `/{id}`          | ADMIN | Cập nhật thông tin khóa học              |
| DELETE | `/{id}`          | ADMIN | Xóa khóa học                            |
| POST   | `/backfill`      | ADMIN | Đồng bộ lại dữ liệu từ Moodle           |

---

### 6.12 Admin — Master Data — `/api/v1/admin/master-data`

| Method | Endpoint               | Auth  | Mô tả                          |
|--------|------------------------|-------|--------------------------------|
| GET    | `/categories`          | ADMIN | Danh sách danh mục             |
| POST   | `/categories`          | ADMIN | Tạo danh mục mới              |
| PUT    | `/categories/{id}`     | ADMIN | Cập nhật danh mục             |
| DELETE | `/categories/{id}`     | ADMIN | Xóa danh mục                  |
| GET    | `/levels`              | ADMIN | Danh sách cấp độ              |
| POST   | `/levels`              | ADMIN | Tạo cấp độ mới               |
| PUT    | `/levels/{id}`         | ADMIN | Cập nhật cấp độ              |
| DELETE | `/levels/{id}`         | ADMIN | Xóa cấp độ                   |

---

### 6.13 Admin — Phân Công Giảng Viên — `/api/v1/admin/course-assignments`

| Method | Endpoint  | Auth  | Mô tả                                  |
|--------|-----------|-------|----------------------------------------|
| GET    | `/`       | ADMIN | Danh sách phân công giảng viên         |
| POST   | `/`       | ADMIN | Phân công giảng viên vào khóa học     |
| DELETE | `/{id}`   | ADMIN | Hủy phân công giảng viên              |

---

### 6.14 Admin — Quản Lý Đăng Ký — `/api/v1/admin/enrollments`

| Method | Endpoint                  | Auth  | Mô tả                                     |
|--------|---------------------------|-------|-------------------------------------------|
| GET    | `/`                       | ADMIN | Danh sách tất cả đăng ký (filter đa dạng)|
| PATCH  | `/{id}/approve`           | ADMIN | Phê duyệt đăng ký                        |
| PATCH  | `/{id}/revoke`            | ADMIN | Thu hồi / từ chối đăng ký                |
| POST   | `/direct-enroll`          | ADMIN | Đăng ký trực tiếp không qua xét duyệt   |

---

### 6.15 Admin — Đồng Bộ Moodle — `/api/v1/admin/moodle`

| Method | Endpoint                      | Auth  | Mô tả                                     |
|--------|-------------------------------|-------|-------------------------------------------|
| GET    | `/diagnose`                   | ADMIN | Kiểm tra trạng thái kết nối Moodle       |
| GET    | `/status`                     | ADMIN | Trạng thái đồng bộ tổng quát            |
| POST   | `/sync-courses`               | ADMIN | Đẩy khóa học từ backend lên Moodle      |
| POST   | `/sync-courses-from-moodle`   | ADMIN | Kéo khóa học từ Moodle về backend       |
| POST   | `/sync-users`                 | ADMIN | Đồng bộ người dùng lên Moodle           |
| POST   | `/import-users`               | ADMIN | Import batch người dùng vào Moodle      |
| POST   | `/import-courses`             | ADMIN | Import batch khóa học vào Moodle        |
| POST   | `/sync-enrollments`           | ADMIN | Đồng bộ đăng ký học viên lên Moodle    |

---

### 6.16 Moodle Integration — `/api/v1/moodle`

| Method | Endpoint                              | Auth    | Mô tả                                      |
|--------|---------------------------------------|---------|---------------------------------------------|
| GET    | `/launch`                             | STUDENT | Lấy SSO URL để vào Moodle với tư cách học viên |
| GET    | `/teacher-launch`                     | TEACHER | Lấy SSO URL với tư cách giảng viên         |
| GET    | `/grades`                             | STUDENT | Lấy bảng điểm từ Moodle gradebook         |
| GET    | `/course-contents`                    | PUBLIC  | Lấy cấu trúc nội dung khóa học (sections) |
| GET    | `/calendar`                           | STUDENT | Lịch học từ Moodle                        |
| GET    | `/timeline`                           | STUDENT | Timeline sự kiện sắp tới                  |
| GET    | `/my-courses`                         | STUDENT | Khóa học đang học trên Moodle             |
| GET    | `/assignments`                        | STUDENT | Danh sách bài tập của khóa học            |
| POST   | `/assignments`                        | STUDENT | Nộp bài tập (text + file)                 |
| GET    | `/quizzes`                            | STUDENT | Danh sách bài kiểm tra                    |
| POST   | `/quizzes/start`                      | STUDENT | Bắt đầu lượt làm bài kiểm tra            |
| POST   | `/quizzes/save`                       | STUDENT | Lưu tạm đáp án                           |
| POST   | `/quizzes/submit`                     | STUDENT | Nộp bài kiểm tra                          |
| GET    | `/quizzes/review`                     | STUDENT | Xem lại kết quả sau khi nộp              |
| GET    | `/quizzes/summary`                    | STUDENT | Tóm tắt tổng quan bài kiểm tra           |
| GET    | `/pages`                              | STUDENT | Xem nội dung trang học (Moodle page)      |
| GET    | `/resources`                          | STUDENT | Xem tài liệu học (Moodle resource)       |
| GET    | `/completion`                         | STUDENT | Trạng thái hoàn thành của hoạt động      |
| POST   | `/completion`                         | STUDENT | Cập nhật trạng thái hoàn thành           |
| GET    | `/user-token`                         | STUDENT | Lấy Moodle token cá nhân                 |
| GET    | `/course-image`                       | PUBLIC  | Lấy ảnh đại diện khóa học từ Moodle     |
| GET    | `/file`                               | PUBLIC  | Proxy file từ Moodle                     |
| POST   | `/file-upload`                        | STUDENT | Upload file lên Moodle                   |

---

## 7. Luồng Hoạt Động Chức Năng

### 7.1 Đăng Ký Tài Khoản

```
[Người dùng] → Nhập username, email, password
      │
      ▼
POST /api/v1/auth/register
      │
      ▼
Backend: Kiểm tra username/email tồn tại
      │ (nếu hợp lệ)
      ▼
Lưu User (role=STUDENT, active=true) vào PostgreSQL
      │
      ▼
Trả về 200 OK (không tự động đăng nhập)
      │
      ▼
Frontend chuyển sang tab Đăng nhập
```

---

### 7.2 Đăng Nhập

```
[Người dùng] → Nhập username/email + password
      │
      ▼
POST /api/v1/auth/login
      │
      ▼
Backend: Xác thực credential → Tạo JWT (HS256, 24h)
      │
      ▼
Frontend: Lưu token vào localStorage["authToken"]
      │
      ▼
Gọi GET /api/v1/users/me → Lưu thông tin user
      │
      ▼
Redirect dựa theo role:
  - ADMIN → /pages/admin/
  - TEACHER → /pages/teacher/
  - STUDENT → /pages/my/dashboard/
```

---

### 7.3 Đặt Lại Mật Khẩu

```
[Người dùng] → Nhập email
      │
      ▼
POST /api/v1/auth/forgot-password  { email }
      │
      ▼
Backend: Tạo reset token (lưu DB với TTL) → Gửi email kèm link
      │
      ▼
[Người dùng] → Click link trong email
      │
Link chứa ?token=<reset_token>
      │
      ▼
POST /api/v1/auth/reset-password  { token, newPassword }
      │
      ▼
Backend: Xác thực token, cập nhật password hash, vô hiệu hóa token
      │
      ▼
Frontend: Thông báo thành công → Redirect đến trang đăng nhập
```

---

### 7.4 Phiên Khách (Guest)

```
[Trang chủ load] → Kiểm tra localStorage["authToken"]
      │ (không có token)
      ▼
POST /api/v1/auth/guest
      │
      ▼
Backend: Tạo JWT guest (role=GUEST, TTL ngắn hơn)
      │
      ▼
Frontend: Lưu guest token → Dùng để gọi PUBLIC endpoints
(Xem khóa học, danh mục, v.v. mà không cần đăng nhập)
```

---

### 7.5 Đăng Ký Khóa Học

```
[Học viên] → Xem chi tiết khóa học (/pages/course/)
      │
GET /api/v1/courses/{id}  (PUBLIC)
      │
      ▼
[Click "Đăng ký"]
      │ (nếu chưa đăng nhập → redirect login)
      ▼
POST /api/v1/courses/enroll/{id}
      │
      ▼
Backend: Tạo Enrollment (status=PENDING)
      │
      ▼
Frontend: Hiện thông báo "Chờ Admin phê duyệt"
      │
      ▼
[Admin] → Xem danh sách PENDING tại /pages/manage/
      │
GET /api/v1/admin/enrollments?status=PENDING
      │
      ▼
[Admin click Phê duyệt]
PATCH /api/v1/admin/enrollments/{id}/approve
      │
      ▼
Backend: 
  1. Cập nhật status=ACTIVE
  2. Gọi Moodle API: Đăng ký học viên vào course trên Moodle
  3. Tạo Notification cho học viên
      │
      ▼
[Học viên] → Nhận thông báo WebSocket / Polling
→ Có thể truy cập khóa học
```

---

### 7.6 Truy Cập Học Tập (SSO Moodle)

```
[Học viên] → Click "Vào học" tại /pages/my/courses/
      │
GET /api/v1/moodle/launch?courseId={id}
      │
      ▼
Backend (MoodleService.launch):
  1. Lấy user info từ DB
  2. Gọi Moodle REST: Kiểm tra user Moodle tồn tại
     - Nếu chưa có → POST /webservice/rest/server.php (core_user_create_users)
  3. Gọi Moodle REST: Kiểm tra enrollment
     - Nếu chưa enrolled → enrol_manual_enrol_users
  4. Tạo SSO auth key (auth_userkey plugin)
  5. Trả về URL: http://moodle/auth/userkey/login.php?key=<key>&wantsurl=<course_url>
      │
      ▼
Frontend: Redirect hoặc mở URL → Người dùng vào Moodle đã đăng nhập
```

---

### 7.7 Giảng Viên Truy Cập Khóa Học

```
[Giảng viên] → Dashboard tại /pages/teacher/
      │
GET /api/v1/teacher/courses  → Danh sách khóa học được phân công
      │
      ▼
[Click "Vào khóa học" để chỉnh sửa nội dung]
GET /api/v1/moodle/teacher-launch?courseId={id}
      │
      ▼
Backend: Tương tự học viên nhưng:
  - Tạo Moodle user với role TEACHER
  - Enrol với role editingteacher trong Moodle
  - SSO URL đến trang chỉnh sửa khóa học Moodle
```

---

### 7.8 Admin Tạo Khóa Học

```
[Admin] → Tạo khóa học tại /pages/admin/
      │
POST /api/v1/admin/courses
  { title, description, categoryId, levelId, price, thumbnailUrl, ... }
      │
      ▼
Backend:
  1. Lưu Course vào PostgreSQL
  2. Gọi Moodle REST: core_course_create_courses
     - Tạo course tương ứng trên Moodle
     - Lưu moodleCourseId vào DB
  3. Trả về course đã tạo (kèm moodleCourseId)
      │
      ▼
[Admin] → Phân công giảng viên
POST /api/v1/admin/course-assignments
  { courseId, teacherId }
      │
      ▼
Backend:
  1. Lưu CourseAssignment vào DB
  2. Gọi Moodle: enrol_manual_enrol_users (role editingteacher)
  3. Tạo Notification cho giảng viên
```

---

### 7.9 Luồng Làm Bài Kiểm Tra (Quiz)

```
[Học viên] → Trang học (/pages/learn/)
→ Chọn Quiz trong sidebar
      │
GET /api/v1/moodle/quizzes?courseId={id}  → Danh sách quiz
      │
      ▼
[Click vào quiz]
GET /api/v1/moodle/quizzes/summary?quizId={id}  → Thông tin quiz (số câu, thời gian, số lần)
      │
      ▼
[Click "Bắt đầu"]
POST /api/v1/moodle/quizzes/start  { quizId }
→ Trả về attemptId + danh sách câu hỏi
      │
      ▼
[Học viên trả lời câu hỏi]
POST /api/v1/moodle/quizzes/save  { attemptId, answers[] }  ← Tự động lưu định kỳ
      │
      ▼
[Click "Nộp bài"]
POST /api/v1/moodle/quizzes/submit  { attemptId }
      │
      ▼
Backend: Gọi Moodle REST mod_quiz_process_attempt (finish=1)
      │
      ▼
GET /api/v1/moodle/quizzes/review?attemptId={id}  → Kết quả chi tiết
```

---

### 7.10 Luồng Nộp Bài Tập (Assignment)

```
[Học viên] → Chọn Assignment trong sidebar trang học
      │
GET /api/v1/moodle/assignments?courseId={id}  → Danh sách bài tập
      │
      ▼
[Học viên soạn bài / upload file]
  Option A: Text submission
  Option B: File upload
    POST /api/v1/moodle/file-upload  { file }  → fileItemId
      │
      ▼
POST /api/v1/moodle/assignments
  { assignmentId, text, fileItemId? }
      │
      ▼
Backend: Gọi Moodle REST mod_assign_save_submission
      │
      ▼
[Học viên] → Xem trạng thái nộp bài (submitted / graded)
```

---

### 7.11 Xem Bảng Điểm

```
[Học viên] → /pages/my/grades/
      │
GET /api/v1/courses/enrolled  → Danh sách khóa học
      │
      ▼
[Chọn khóa học từ dropdown]
GET /api/v1/moodle/grades?courseId={id}
      │
      ▼
Backend: Gọi Moodle REST gradereport_user_get_grade_items
→ Lấy điểm từng hoạt động (quiz, assignment, v.v.)
      │
      ▼
Frontend: Render bảng điểm theo cấu trúc Moodle gradebook
```

---

### 7.12 Hệ Thống Thông Báo

```
[Trang load] → initNavbar() kiểm tra auth
      │ (đã đăng nhập)
      ▼
GET /api/v1/notifications/unread-count  → Hiện badge số
      │
[Click chuông thông báo]
      ▼
GET /api/v1/notifications  → Danh sách 20 thông báo mới nhất
      │
      ▼
PUT /api/v1/notifications/read-all  ← Tự động đánh dấu đã đọc
      │
[Thời gian thực — WebSocket]
      ▼
ws://localhost:4000/ws (STOMP over SockJS)
Subscribe: /user/queue/notifications
→ Server push khi có thông báo mới (approve enrollment, v.v.)
```

---

### 7.13 Admin Đồng Bộ Moodle

```
[Admin] → Trang Admin → Tab "Đồng bộ Moodle"
      │
GET /api/v1/admin/moodle/status  → Kiểm tra kết nối + số liệu
      │
      ▼
Các thao tác đồng bộ (thủ công):

[Sync Courses → Moodle]
POST /api/v1/admin/moodle/sync-courses
→ Duyệt tất cả courses trong DB chưa có moodleCourseId
→ Gọi Moodle API tạo từng course

[Sync Users → Moodle]  
POST /api/v1/admin/moodle/sync-users
→ Duyệt tất cả users chưa có moodleUserId
→ Gọi Moodle API tạo từng user

[Sync Enrollments]
POST /api/v1/admin/moodle/sync-enrollments
→ Duyệt enrollments ACTIVE chưa sync
→ Gọi Moodle API đăng ký học viên vào course tương ứng
```

---

### 7.14 Thanh Toán

```
[Học viên] → Click "Mua khóa học" (có giá)
      │
POST /api/v1/payments/initiate  { courseId }
      │
      ▼
Backend: Tạo Payment record (status=PENDING)
→ Tạo URL thanh toán từ cổng (VNPay/MoMo/v.v.)
→ Trả về payment URL
      │
      ▼
Frontend: Redirect đến cổng thanh toán
      │
      ▼
[Cổng thanh toán] → Callback sau khi xử lý
POST /api/v1/payments/callback  (PUBLIC — do cổng gọi)
      │
      ▼
Backend: 
  - Xác thực chữ ký callback
  - Cập nhật Payment status (SUCCESS/FAILED)
  - Nếu SUCCESS → Tạo Enrollment (status=ACTIVE) + Sync Moodle
      │
      ▼
[Học viên] → Nhận thông báo + Có thể vào học ngay
```

---

## 8. Tích Hợp Moodle LMS

### Cấu Hình Kết Nối

```properties
# application.properties
moodle.url=http://localhost:8088
moodle.admin-token=<admin_webservice_token>
moodle.wsfunction.base=?wstoken={token}&moodlewsrestformat=json
```

### Plugin Bắt Buộc

| Plugin               | Chức năng                                  |
|----------------------|--------------------------------------------|
| `auth_userkey`       | SSO — tạo key đăng nhập 1 lần             |
| `webservice`         | REST API endpoint `/webservice/rest/server.php` |
| `local_wsmanagegroups` | (tùy chọn) Quản lý nhóm học viên        |

### Các Moodle REST Function Sử Dụng

| Moodle Function                        | Mục đích                            |
|----------------------------------------|-------------------------------------|
| `core_user_create_users`               | Tạo người dùng Moodle              |
| `core_user_get_users`                  | Kiểm tra user tồn tại              |
| `core_user_update_users`               | Cập nhật thông tin user            |
| `core_course_create_courses`           | Tạo khóa học                       |
| `core_course_get_courses`              | Lấy danh sách khóa học             |
| `core_course_get_contents`             | Lấy nội dung khóa học (sections)  |
| `enrol_manual_enrol_users`             | Đăng ký người dùng vào khóa học   |
| `enrol_manual_unenrol_users`           | Hủy đăng ký                        |
| `mod_quiz_get_quizzes_by_courses`      | Lấy danh sách quiz                 |
| `mod_quiz_start_attempt`               | Bắt đầu làm quiz                   |
| `mod_quiz_process_attempt`             | Lưu/nộp bài quiz                   |
| `mod_quiz_get_attempt_review`          | Xem lại kết quả quiz               |
| `mod_assign_get_assignments`           | Lấy danh sách bài tập              |
| `mod_assign_save_submission`           | Nộp bài tập                        |
| `gradereport_user_get_grade_items`     | Lấy điểm học viên                  |
| `auth_userkey_request_login_url`       | Tạo SSO login URL                  |
| `core_calendar_get_calendar_events`    | Lấy lịch học                       |

---

## 9. Cơ Sở Dữ Liệu

### Schema Chính (PostgreSQL)

```sql
-- Người dùng
users (id, username, email, password_hash, role, active, moodle_user_id, created_at)

-- Danh mục & Cấp độ
categories (id, name, description, icon)
levels (id, name, description, order_index)

-- Khóa học
courses (id, title, description, category_id, level_id, price, 
         thumbnail_url, moodle_course_id, status, created_at)

-- Phân công giảng viên
course_assignments (id, course_id, teacher_id, assigned_at)

-- Đăng ký học
enrollments (id, user_id, course_id, status [PENDING/ACTIVE/REVOKED], 
             enrolled_at, approved_at, approved_by)

-- Đánh giá
reviews (id, user_id, course_id, rating, comment, created_at)

-- Thanh toán
payments (id, user_id, course_id, amount, status [PENDING/SUCCESS/FAILED],
          transaction_id, created_at)

-- Thông báo
notifications (id, user_id, title, message, type, read, created_at)

-- Reset password tokens
password_reset_tokens (id, user_id, token, expires_at, used)
```

### Migration (Flyway)

Các file migration đặt tại `src/main/resources/db/migration/`:
- `V1__init_schema.sql` — Schema ban đầu
- `V2__add_roles.sql` → `V23__...` — Các thay đổi tăng dần

---

## 10. WebSocket & Thông Báo Thời Gian Thực

### Kết Nối

```javascript
// Frontend (SockJS + STOMP)
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({ 'Authorization': `Bearer ${token}` }, () => {
  stompClient.subscribe('/user/queue/notifications', (msg) => {
    const notification = JSON.parse(msg.body);
    // Cập nhật UI
  });
});
```

### Các Topic WebSocket

| Topic                          | Mô tả                                     |
|--------------------------------|-------------------------------------------|
| `/user/queue/notifications`    | Thông báo cá nhân (mỗi user)             |
| `/topic/announcements`         | Thông báo chung toàn hệ thống (nếu có)   |

### Khi Nào Thông Báo Được Gửi

| Sự kiện                          | Người nhận   |
|----------------------------------|--------------|
| Enrollment được phê duyệt       | Học viên     |
| Bị phân công khóa học           | Giảng viên   |
| Học viên nộp bài                | Giảng viên   |
| Điểm được chấm                  | Học viên     |

---

## 11. Lưu Trữ Tệp (MinIO)

- **Endpoint**: `http://localhost:9000`
- **Bucket chính**: `sunshine-uploads`
- **Dùng để**: Upload ảnh đại diện khóa học, ảnh profile, file đính kèm bài tập
- **URL ảnh**: Trả về dưới dạng pre-signed URL hoặc public URL

```java
// Backend: Upload ảnh khóa học
POST /api/v1/admin/courses → body chứa thumbnailUrl (URL MinIO)
// Hoặc upload trực tiếp qua presigned URL
```

---

## 12. Cấu Hình Triển Khai

### Docker Compose (Production)

```yaml
# docker-compose.prod.yml
services:
  nginx:      # Port 80/443 — Reverse proxy
  backend:    # Port 4000 — Spring Boot API
  postgres:   # Port 5432 — Database
  redis:      # Port 6379 — JWT blacklist cache
  moodle:     # Port 8088 — Moodle LMS
  minio:      # Port 9000/9001 — File storage
```

### Biến Môi Trường Backend Quan Trọng

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sso_db
SPRING_DATASOURCE_USERNAME=sso_user
SPRING_DATASOURCE_PASSWORD=...

# JWT
JWT_SECRET=<256-bit secret>
JWT_EXPIRATION=86400000  # 24h in ms

# Redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Moodle
MOODLE_URL=http://moodle:8088
MOODLE_ADMIN_TOKEN=...

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
```

### Nginx Routing

```nginx
# /api/* → Backend (port 4000)
location /api/ {
  proxy_pass http://backend:4000;
}

# /ws → WebSocket Backend
location /ws {
  proxy_pass http://backend:4000;
  proxy_http_version 1.1;
  proxy_set_header Upgrade $http_upgrade;
  proxy_set_header Connection "upgrade";
}

# /* → Frontend static files
location / {
  root /usr/share/nginx/html;
  try_files $uri $uri/ /index.html;
}
```

---

*Tài liệu này phản ánh trạng thái hệ thống tại thời điểm phân tích. Mọi thay đổi về API hoặc luồng nghiệp vụ cần được cập nhật tương ứng.*
