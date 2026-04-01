# EnglishEdu Backend — Tổng Quan Kỹ Thuật

> **Dự án**: SSO (Sunshine School Online) — Backend API cho nền tảng học tiếng Anh trực tuyến  
> **Ngày cập nhật**: 18/03/2026

---

## 1. Tech Stack & Dependencies

| Thành phần | Thư viện / Phiên bản | Vai trò |
|---|---|---|
| Framework | **Spring Boot 4.0.3** | Core framework |
| Ngôn ngữ | **Java 17** | |
| Build | **Maven** | Quản lý dependency & build |
| ORM | Spring Data JPA + Hibernate | Tương tác database |
| Database | **PostgreSQL** | Lưu trữ chính |
| Migrations | **Flyway** | Quản lý schema versioning |
| Cache | **Spring Data Redis** | Blacklist JWT khi logout |
| Email | Spring Mail | Gửi email reset mật khẩu |
| Security | Spring Security (stateless) | Xác thực & phân quyền |
| JWT | **JJWT 0.12.6** | Tạo và xác thực token |
| Object Mapping | **MapStruct 1.6.2** | Entity ↔ DTO mapping |
| API Docs | **SpringDoc OpenAPI 2.8.6** | Swagger UI |
| File Storage | **MinIO 8.5.12** | Lưu trữ file (avatar) |
| Boilerplate | Lombok | Giảm code boilerplate |
| Validation | Jakarta Bean Validation | Validate request DTOs |
| Testing | Spring Boot Test, H2 | Unit & integration test |

---

## 2. Cấu Hình Khởi Động

| Thông số | Giá trị |
|---|---|
| **Port** | `4000` |
| **Base API path** | `/api/v1/` |
| **Swagger UI** | `/swagger-ui/index.html` |
| **OpenAPI JSON** | `/v3/api-docs` |

---

## 3. Docker Setup (`docker-compose.yml`)

| Service | Image | Port | Thông tin |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `5432` | DB: `sso_db`, user: `sso_user`, pass: `sso_pass` |
| `redis` | `redis:7-alpine` | `6379` | Lưu blacklist token |
| `minio` | `minio/minio:latest` | `9000` (API), `9001` (Console) | Lưu file; creds: `minioadmin/minioadmin` |

Persistent volumes: `pgdata`, `miniodata`.

> ⚠️ **Lưu ý**: `application.properties` kết nối tới DB `EnglishEdu` (không phải `sso_db`). Tên DB local dev khác với docker-compose.

---

## 4. Database Schema

### Bảng `users` (V1)
```sql
id          BIGSERIAL PRIMARY KEY
username    VARCHAR(50)  NOT NULL UNIQUE
email       VARCHAR(255) NOT NULL UNIQUE
password    VARCHAR(255) NOT NULL           -- BCrypt hashed
first_name  VARCHAR(100)
last_name   VARCHAR(100)
avatar_url  VARCHAR(512)
role        VARCHAR(20)  DEFAULT 'STUDENT'  -- STUDENT | ADMIN
is_active   BOOLEAN      DEFAULT TRUE
is_guest    BOOLEAN      DEFAULT FALSE
created_at  TIMESTAMP    DEFAULT NOW()
updated_at  TIMESTAMP    DEFAULT NOW()
```

### Bảng `courses` (V2)
```sql
id           BIGSERIAL PRIMARY KEY
external_id  INTEGER UNIQUE               -- ID khóa học trong Moodle LMS
name         VARCHAR(255) NOT NULL
category     VARCHAR(50)  NOT NULL        -- 'IELTS' | 'CAMBRIDGE'
level        VARCHAR(50)                  -- Beginner / A2 / B1 / ...
description  TEXT
image_url    VARCHAR(512)
is_published BOOLEAN      DEFAULT TRUE
created_at   TIMESTAMP    DEFAULT NOW()
```

### Bảng `enrollments` (V3)
```sql
id            BIGSERIAL PRIMARY KEY
user_id       BIGINT → users(id)   ON DELETE CASCADE
course_id     BIGINT → courses(id) ON DELETE CASCADE
status        VARCHAR(20) DEFAULT 'inprogress'
progress      INTEGER     DEFAULT 0        -- 0–100
enrolled_at   TIMESTAMP   DEFAULT NOW()
last_accessed TIMESTAMP
UNIQUE(user_id, course_id)
```

### Bảng `timeline_events` (V4)
```sql
id           BIGSERIAL PRIMARY KEY
user_id      BIGINT → users(id)   ON DELETE CASCADE
course_id    BIGINT → courses(id) ON DELETE SET NULL
title        VARCHAR(255) NOT NULL
type         VARCHAR(50)
due_at       TIMESTAMP
completed_at TIMESTAMP
created_at   TIMESTAMP DEFAULT NOW()
```

### Bảng `notifications` (V5)
```sql
id         BIGSERIAL PRIMARY KEY
user_id    BIGINT → users(id) ON DELETE CASCADE
message    TEXT NOT NULL
link       VARCHAR(512)
is_read    BOOLEAN DEFAULT FALSE
created_at TIMESTAMP DEFAULT NOW()
```

### Bảng `calendar_events` (V6)
```sql
id          BIGSERIAL PRIMARY KEY
user_id     BIGINT → users(id) ON DELETE CASCADE
title       VARCHAR(255) NOT NULL
description TEXT
event_date  DATE NOT NULL
created_at  TIMESTAMP DEFAULT NOW()
```

### Bảng `password_reset_tokens` (V7)
```sql
id         BIGSERIAL PRIMARY KEY
user_id    BIGINT → users(id) ON DELETE CASCADE
token      VARCHAR(255) NOT NULL UNIQUE
expires_at TIMESTAMP NOT NULL              -- TTL: 1 giờ
used       BOOLEAN DEFAULT FALSE
created_at TIMESTAMP DEFAULT NOW()
```

### Seed Data (V8)
24 khóa học được seed sẵn:
- **IELTS**: 14 khóa (external_ids: 27, 30, 33, 39, 40, 41, 42, 68, 74, 76, 81, 82, 83, 84)
- **Cambridge**: 10 khóa A2→B2 (external_ids: 25, 29, 38, 66, 67, 69, 70, 71, 72, 73)

---

## 5. Tất Cả REST API Endpoints

### Auth — `/api/v1/auth` (PUBLIC)

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Đăng nhập → trả JWT token |
| `POST` | `/api/v1/auth/guest` | Tạo tài khoản guest ẩn danh → JWT token |
| `POST` | `/api/v1/auth/forgot-password` | Gửi email đặt lại mật khẩu |
| `POST` | `/api/v1/auth/logout` | Blacklist token trong Redis |

### Users — `/api/v1/users/me` (PROTECTED)

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/v1/users/me` | Lấy thông tin cá nhân |
| `PUT` | `/api/v1/users/me` | Cập nhật firstName, lastName, email |
| `POST` | `/api/v1/users/me/password` | Đổi mật khẩu (yêu cầu mật khẩu hiện tại) |
| `POST` | `/api/v1/users/me/avatar` | Upload avatar `multipart/form-data` → MinIO |

### Courses — `/api/v1/courses`

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/courses` | PUBLIC | Danh sách khóa học đã xuất bản (phân trang, `?category=`) |
| `GET` | `/api/v1/courses/{id}` | PUBLIC | Chi tiết một khóa học |
| `GET` | `/api/v1/courses/enrolled` | **Protected** | Khóa học đang học của user |
| `GET` | `/api/v1/courses/recent` | **Protected** | 5 khóa học truy cập gần nhất |
| `GET` | `/api/v1/courses/dashboard` | **Protected** | Thống kê: tổng / đang học / hoàn thành |
| `POST` | `/api/v1/courses/{id}/enroll` | **Protected** | Đăng ký học một khóa học |
| `PATCH` | `/api/v1/courses/{id}/enrollment` | **Protected** | Cập nhật progress (0–100) và/hoặc status |

### Calendar — `/api/v1/calendar` (PROTECTED)

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/v1/calendar` | Lấy sự kiện theo `?year=&month=` |
| `POST` | `/api/v1/calendar` | Tạo sự kiện lịch |
| `DELETE` | `/api/v1/calendar/{id}` | Xóa sự kiện của mình |

### Notifications — `/api/v1/notifications` (PROTECTED)

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/v1/notifications` | Danh sách thông báo (mới nhất trước) |
| `PUT` | `/api/v1/notifications/read-all` | Đánh dấu tất cả đã đọc |
| `DELETE` | `/api/v1/notifications/{id}` | Xóa một thông báo |

### Timeline — `/api/v1/timeline` (PROTECTED)

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/v1/timeline` | Lấy sự kiện timeline (`?days=7&sort=upcoming\|overdue`) |

---

## 6. Xác Thực & Phân Quyền (JWT + Spring Security)

### Luồng JWT
- **Algorithm**: HMAC-SHA256 (`HS256`) qua JJWT
- **Secret**: lấy từ `jwt.secret` trong `application.properties`
- **Thời hạn**: `86400000ms` = **24 giờ**
- **Payload**: `sub` = userId (string), `username` claim, `iat`, `exp`

### Filter Chain (`JwtAuthenticationFilter`)
Mỗi request được xử lý theo thứ tự:
1. Trích xuất `Bearer <token>` từ header `Authorization`
2. Validate chữ ký JWT và hạn sử dụng
3. Kiểm tra Redis `blacklist:<token>` — nếu có → từ chối
4. Load user từ DB bằng `userId` → set `SecurityContext`

### Phân Quyền Endpoints
```
/api/v1/auth/**                   → permitAll
GET /api/v1/courses               → permitAll
GET /api/v1/courses/{id}          → permitAll
/swagger-ui/**, /v3/api-docs/**   → permitAll
/actuator/health                  → permitAll
Còn lại                           → authenticated
```

Session: **STATELESS** (không dùng HttpSession) | CSRF: **disabled**

### Roles
| Role | Mô tả |
|---|---|
| `STUDENT` | Mặc định khi đăng ký |
| `ADMIN` | Tạo tự động bởi `DataInitializer` |

`DataInitializer` tạo tài khoản `admin / Admin@123` khi khởi động nếu chưa tồn tại.

---

## 7. Redis — JWT Blacklist

Redis chỉ được dùng cho một mục đích: **blacklist token khi logout**.

```java
// Khi logout:
redisTemplate.opsForValue().set(
    "blacklist:" + token,
    "true",
    tokenExpirationMs,          // TTL tự động xóa sau 24h
    TimeUnit.MILLISECONDS
);

// Khi xác thực:
redisTemplate.hasKey("blacklist:" + token)  // → từ chối nếu true
```

---

## 8. File Storage — MinIO

`StorageConfig` tạo `MinioClient` bean và tự khởi tạo bucket `sso-uploads` nếu chưa tồn tại.

**Luồng upload avatar**:
1. Sinh tên file: `avatars/{uuid}{extension}`
2. Upload qua `PutObjectArgs` với content-type gốc
3. Trả về **pre-signed GET URL** có thời hạn **7 ngày**

---

## 9. Service Layer

### `AuthService`
- **login**: `AuthenticationManager.authenticate()` → load user → generate JWT → map response
- **guestLogin**: tạo user với `guest_XXXXXXXX` username ngẫu nhiên, password ngẫu nhiên, `is_guest=true` → trả JWT
- **forgotPassword**: tìm user bằng `username OR email` → tạo UUID token (TTL 1h) → lưu DB → gửi email
- **logout**: blacklist token trong Redis với TTL khớp JWT

### `UserService`
- **getProfile**: lấy user theo id → map sang `UserResponse`
- **updateProfile**: partial update, chỉ set các field không-null; kiểm tra email trùng lặp
- **changePassword**: xác minh mật khẩu hiện tại bằng `BCrypt.matches()` trước khi đổi
- **uploadAvatar**: upload MinIO → lưu URL vào bảng `users`

### `CourseService`
- **getCourses**: phân trang (mặc định 12/trang), filter theo `category` (case-insensitive)
- **getCourse**: tìm theo DB id, ném `ResourceNotFoundException` nếu không tồn tại

### `EnrollmentService`
- **getEnrolledCourses**: sắp xếp theo `lastAccessed DESC`
- **getRecentCourses**: lấy 5 khóa gần nhất bằng Java `.limit(5)`
- **getDashboardStats**: đếm 3 giá trị: tổng, in_progress, completed (`progress = 100`)
- **enroll**: kiểm tra trùng, ném `BadRequestException` nếu đã đăng ký
- **updateEnrollment**: partial update `status` và/hoặc `progress`, set `lastAccessed = now()`

### `CalendarService`
- Lấy sự kiện của tháng (từ ngày 1 đến ngày cuối tháng)
- Delete kiểm tra ownership: ném exception nếu không phải của user

### `NotificationService`
- `markAllAsRead`: một câu JPQL `UPDATE` bulk

### `TimelineService`
- `upcoming`: pending events (`completedAt IS NULL`) trong khoảng `[now-days, now+days]`
- `overdue`: pending events với `dueAt < now`

### `EmailService`
- Dùng `JavaMailSender` để gửi plain text email
- Reset link: `http://localhost:3000/login/forgot-password.html?token={token}` *(hardcoded — cần thay bằng env var cho production)*

---

## 10. Exception Handling

`@RestControllerAdvice` (`GlobalExceptionHandler`) xử lý tập trung:

| Exception | HTTP Status | Response |
|---|---|---|
| `ResourceNotFoundException` | `404 Not Found` | `ApiResponse.error(message)` |
| `BadRequestException` | `400 Bad Request` | `ApiResponse.error(message)` |
| `BadCredentialsException` | `401 Unauthorized` | `"Invalid username or password"` |
| `MethodArgumentNotValidException` | `400 Bad Request` | Danh sách lỗi validation |
| `Exception` (catch-all) | `500 Internal Server Error` | `"Internal server error"` |

Mọi response dùng wrapper `ApiResponse<T>`.

---

## 11. DTOs

### Request DTOs

| DTO | Fields |
|---|---|
| `LoginRequest` | `username` @NotBlank, `password` @NotBlank |
| `ForgotPasswordRequest` | `search` @NotBlank (username hoặc email) |
| `UpdateProfileRequest` | `firstName` @Size(max=100), `lastName` @Size(max=100), `email` @Email |
| `ChangePasswordRequest` | `currentPassword` @NotBlank, `newPassword` @NotBlank @Size(min=8) |
| `CreateCalendarEventRequest` | `title` @NotBlank, `description`, `eventDate` @NotNull |
| `UpdateEnrollmentRequest` | `status`, `progress` @Min(0) @Max(100) |

### Response DTOs

| DTO | Mô tả | Fields chính |
|---|---|---|
| `ApiResponse<T>` | Wrapper universal | `success`, `message`, `data` |
| `AuthResponse` | Kết quả login | `token`, `user: UserResponse` |
| `UserResponse` | Thông tin user | `id`, `username`, `email`, `firstName`, `lastName`, `avatarUrl`, `role`, `guest`, `createdAt` |
| `CourseResponse` | Thông tin khóa học | `id`, `externalId`, `name`, `category`, `level`, `description`, `imageUrl` |
| `EnrolledCourseResponse` | Khóa học đang học | `enrollmentId`, `courseId`, `externalId`, `name`, `category`, `level`, `imageUrl`, `progress`, `status`, `lastAccessed` |
| `DashboardResponse` | Thống kê dashboard | `totalEnrolled`, `inProgress`, `completed` |
| `CalendarEventResponse` | Sự kiện lịch | `id`, `title`, `description`, `eventDate` |
| `NotificationResponse` | Thông báo | `id`, `message`, `link`, `read`, `createdAt` |
| `TimelineEventResponse` | Timeline | `id`, `courseId`, `courseName`, `title`, `type`, `dueAt`, `completedAt` |

---

## 12. MapStruct Mappers

| Mapper | Mapping |
|---|---|
| `UserMapper` | `User → UserResponse` (direct field mapping) |
| `CourseMapper` | `Course → CourseResponse`, `Enrollment → EnrolledCourseResponse` (flatten `enrollment.course.*`) |

---

## 13. CORS & Security Configuration

```java
// Allowed origins (từ SecurityConfig)
- http://localhost:3000
- http://localhost:5173   // Vite dev server
- https://sunshineschool.edu.vn
```

Các methods được phép: `GET, POST, PUT, PATCH, DELETE, OPTIONS`  
Allowed headers: `Authorization, Content-Type`  
`allowCredentials: true`

---

## 14. Cấu Trúc Package

```
com.sso/
├── SsoApplication.java
├── config/
│   ├── SecurityConfig.java       ← Spring Security + CORS + filter chain
│   ├── JwtProperties.java        ← @ConfigurationProperties jwt.*
│   ├── OpenApiConfig.java        ← Swagger Bearer auth scheme
│   ├── RedisConfig.java          ← StringRedisTemplate bean
│   ├── StorageConfig.java        ← MinioClient bean + create bucket
│   └── DataInitializer.java      ← Seed admin user khi startup
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── CourseController.java
│   ├── CalendarController.java
│   ├── NotificationController.java
│   └── TimelineController.java
├── dto/
│   ├── request/                  ← *Request.java
│   └── response/                 ← *Response.java
├── entity/                       ← JPA entities
├── repository/                   ← Spring Data JPA interfaces
├── service/                      ← Business logic
├── mapper/                       ← MapStruct interfaces
├── security/
│   ├── JwtTokenProvider.java     ← generate/validate/parse JWT
│   └── JwtAuthenticationFilter.java ← OncePerRequestFilter
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BadRequestException.java
└── util/                         ← Utility classes
```

---

*Xem thêm: [FRONTEND.md](FRONTEND.md) — Tài liệu Frontend*
