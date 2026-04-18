# CHANGELOG — EnglishEdu

> Theo dõi tất cả thay đổi code + server kể từ khi deploy  
> IP server: `14.225.217.172` · Moodle: `http://14.225.217.172:8080`

---

## [Unreleased] — Fix Moodle User Sync Deadlock (2026-04-17)

### Bugs Fixed

#### `MoodleSyncService.java` — Deadlock khi tạo user
- **Fix HIGH:** `ensureMoodleUser()` dùng `@Transactional(REQUIRES_NEW)` và gọi `userRepository.save(user)`.
  Khi được gọi từ `createUser()` hoặc `register()` (đang giữ write-lock trên row user chưa commit) → **deadlock/timeout**, exception bị catch silent → user được tạo nhưng **không bao giờ đồng bộ lên Moodle**.
- **Fix:** Tách ra method `provisionMoodleUser(user)` — không có `@Transactional`, chỉ gọi Moodle API + set field trên entity, **không tự save**. Caller chịu trách nhiệm save trong transaction của mình.
- `ensureMoodleUser()` giữ nguyên cho các caller khác (SSO, enrolment) nhưng chuyển thành wrapper gọi `provisionMoodleUser()` rồi save.

#### `UserService.createUser()` — Không sync Moodle
- **Fix HIGH:** Chuyển từ `ensureMoodleUser(saved)` → `provisionMoodleUser(saved)` + `userRepository.save(saved)` trong cùng transaction. Log rõ moodleId sau khi sync thành công.

#### `AuthService.register()` — Không sync Moodle
- **Fix HIGH:** Tương tự `createUser()` — chuyển sang `provisionMoodleUser()` để tránh deadlock.

#### `MoodleSyncService.syncAllUsers()` — Đếm sai, không save
- **Fix MEDIUM:** Chuyển từ `ensureMoodleUser(u)` → `provisionMoodleUser(u)` + `userRepository.save(u)` để tránh nested `REQUIRES_NEW` trong `@Transactional` ngoài. Thêm log rõ moodleId sau từng sync.

### Frontend — `pages/admin/users/users.ts`
- **Thêm:** Icon Moodle sync status trong bảng users — 🔗 (đã sync, hiện moodleId khi hover) / ⚠ (chưa sync)
- **Thêm:** Toast khi tạo user mới hiển thị Moodle ID nếu sync thành công hoặc cảnh báo nếu thất bại

---

## [Unreleased] — Code Quality & Test Coverage (2026-04-13)

### Bảo mật

#### `GlobalExceptionHandler.java` — Ẩn lỗi nội bộ Moodle
- **Fix MEDIUM:** Handler `MoodleApiException` trả về `"Moodle error: " + ex.getMessage()` → lộ chi tiết nội bộ Moodle cho client
- Đổi thành thông báo generic tiếng Việt; giữ log chi tiết ở server-side

#### `RegisterRequest.java` — Username không được là số thuần túy
- **Fix LOW:** Username regex `^[a-zA-Z0-9_.-]+$` cho phép username như `"123"` → xung đột với pattern `Long.parseLong(username)` trong controller
- Đổi regex → bắt buộc ít nhất 1 chữ cái: `^(?=.*[a-zA-Z])[a-zA-Z0-9_.-]+$`

### Bugs

#### `CourseAssignmentService.java` — getReferenceById → findById
- **Fix HIGH:** `userRepository.getReferenceById(adminId)` trả JPA proxy → nổ exception khi adminId không tồn tại và entity được access
- Đổi sang `findById(adminId).orElseThrow(() -> new ResourceNotFoundException(...))`

#### `CourseService.java` — Teacher update thất bại âm thầm
- **Fix MEDIUM:** `userRepository.findById(teacherId).ifPresent(...)` → nếu teacher không tồn tại, course silently không được cập nhật teacher
- Đổi sang `.orElseThrow(() -> new ResourceNotFoundException("Teacher not found"))`; đồng thời thêm import `User`

### Tests

#### Thêm 3 test suite mới (+26 tests)
- **AuthServiceTest** (9 tests): `register`, `login`, `guestLogin`, `forgotPassword` — bao gồm happy path + edge cases (trùng username/email, bad credentials, Moodle sync failure không abort)
- **PaymentServiceTest** (9 tests): `initiatePayment`, `handlePaymentCallback` — idempotency, free course, enrollment activation, FAILED status
- **ReviewServiceTest** (8 tests): `createReview` (enrollment required, 30% progress gate, duplicate check), `updateReview`, `deleteReview`

**Tổng số tests: 65 (tăng từ 39)**

---

## [Unreleased] — Security Audit & Optimization (2026-04-09)

### Bảo mật (Security)

#### `PaymentService.java` — Idempotency check
- **Fix CRITICAL:** `handlePaymentCallback()` không kiểm tra trạng thái hiện tại → webhook replay dẫn đến kích hoạt enrollment trùng lặp
- Thêm guard: nếu payment đã `COMPLETED` hoặc `FAILED` → trả về ngay, không xử lý lại

#### `StorageService.java` — File upload validation
- **Fix HIGH:** Không validate loại file và kích thước → cho phép upload file nguy hiểm
- Thêm whitelist extension (jpg, png, pdf, doc, mp3, mp4...)
- Thêm giới hạn kích thước tối đa 10 MB

#### `JwtProperties.java` — JWT secret validation
- **Fix CRITICAL:** Thêm `@PostConstruct` kiểm tra `JWT_SECRET` phải >= 32 ký tự, không được rỗng
- App sẽ fail-fast nếu thiếu JWT_SECRET thay vì chạy không an toàn

#### `MoodleProperties.java` — Startup warning
- **Fix CRITICAL:** Thêm `@PostConstruct` cảnh báo nếu `MOODLE_TOKEN` rỗng hoặc `MOODLE_SSO_SECRET` là giá trị mặc định

#### `application.properties` — Xóa credentials mặc định
- **Fix CRITICAL:** Xóa password mặc định `GilArchon` trong datasource (`DB_PASSWORD` giờ là rỗng nếu không set env)
- **Fix CRITICAL:** Xóa JWT secret mặc định (giờ bắt buộc set `JWT_SECRET` env var)
- **Fix HIGH:** Xóa `minioadmin` default cho MinIO access/secret key
- **Fix LOW:** Ẩn actuator `info` endpoint (chỉ expose `health`)

#### `pom.xml` — Xóa hardcoded credentials
- **Fix CRITICAL:** Flyway plugin có hardcoded DB password `GilArchon` trong source — đổi sang dùng env vars `${env.DB_URL}`, `${env.DB_USERNAME}`, `${env.DB_PASSWORD}`

#### `SecurityConfig.java` — CORS headers whitelist
- **Fix MEDIUM:** `setAllowedHeaders(*)` → whitelist cụ thể: `Content-Type`, `Authorization`, `Accept`, `X-Requested-With`

#### `.env.prod.example` — Xóa credentials thật
- **Fix CRITICAL:** File chứa email thật, mật khẩu thật, JWT secret thật → thay toàn bộ bằng placeholder `CHANGE_ME_*`

### Stability & Error Handling

#### `EmailService.java` — Error handling
- **Fix MEDIUM:** `mailSender.send()` không bắt exception → lỗi 500 không rõ ràng khi mail server hỏng
- Thêm try/catch MailException với log + thông báo user-friendly

#### `NotificationPushService.java` — Safe user lookup
- **Fix HIGH:** `getReferenceById(userId)` trả proxy → nổ LazyInitializationException khi user không tồn tại
- Đổi sang `findById()` + log warning nếu user không tìm thấy (không throw)

#### `EnrollmentService.java` — getReferenceById → findById
- **Fix HIGH:** 3 chỗ dùng `getReferenceById(adminId)` → đổi tất cả sang `findById()` + `orElseThrow()`

### Infrastructure

#### `docker-compose.prod.yml` — Health checks
- **Thêm:** Health check cho backend (`/actuator/health`), PostgreSQL (`pg_isready`), Redis (`redis-cli ping`)
- **Fix:** Backend `depends_on` đổi từ list → condition `service_healthy` cho postgres và redis

#### `nginx.conf` — Security headers + request limit
- **Thêm:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`, `X-XSS-Protection`, `Referrer-Policy`
- **Thêm:** `client_max_body_size 10m` (giới hạn upload qua nginx)

---

## [Unreleased] — Cần deploy lên server

### Fix critical: 502 Bad Gateway

#### `MoodleClient.java`
- **Fix CRITICAL:** `RestClient` khởi tạo trong constructor → crash nếu `MOODLE_URL` chưa set → chuyển sang **lazy-init** (chỉ tạo khi thực sự gọi API file)
- Thêm null-check + error message rõ ràng khi MOODLE_URL chưa cấu hình

#### `MoodleProperties.java`
- **Fix:** Đổi `@Configuration` → `@Component` (đúng convention cho `@ConfigurationProperties`)

#### `.env.prod.example`
- **Fix:** Cập nhật IP từ `14.225.192.133` → `14.225.217.172`

#### `DEPLOY-GUIDE.md`
- **Fix:** Cập nhật IP server

### Thay đổi code enrollment sync

#### `EnrollmentService.java`
- **Fix:** `getEnrolledCourses()` — Nếu student chưa có `moodleId`, tự động gọi `ensureMoodleUser()` trước khi sync (trước đây skip silent)
- **Fix:** `syncMoodleEnrollmentsToLocal()` — Tự động import course từ Moodle vào DB local nếu chưa có (trước đây bỏ qua course chỉ tồn tại trên Moodle)
- **Fix:** `revokeEnrollment()` — Gọi `unenrolStudent()` để đồng bộ thu hồi sang Moodle

#### `MoodleSyncService.java`
- **Thêm:** `importMoodleCourses()` — Import toàn bộ course từ Moodle vào DB local (batch)
- **Thêm:** `syncAllEnrollmentsFromMoodle()` — Sync toàn bộ enrollment từ Moodle cho mọi student
- **Thêm:** `unenrolStudent(User, Course)` — Bỏ ghi danh student khỏi Moodle
- **Thêm:** dependency `EnrollmentRepository`

#### `MoodleClient.java`
- **Thêm:** `getAllCourses()` — Gọi `core_course_get_courses`
- **Thêm:** `unenrolUser(moodleUserId, moodleCourseId)` — Gọi `enrol_manual_unenrol_users`

#### `MoodleController.java`
- **Thêm:** `POST /api/v1/admin/moodle/import-courses` — Import course từ Moodle
- **Thêm:** `POST /api/v1/admin/moodle/sync-enrollments` — Sync toàn bộ enrollment từ Moodle
- **Fix:** `GET /moodle/launch` — Student giờ được auto-enroll trên Moodle khi launch
- **Fix:** `GET /moodle/teacher-launch` — `moodleBaseUrl` trả về `publicUrl` thay vì Docker-internal URL

---

### Việc cần làm thủ công trên server sau khi deploy

#### ① Thêm function `enrol_manual_unenrol_users` vào Moodle service

Truy cập server, chạy lệnh vào MariaDB:

```bash
# Vào container MariaDB
docker exec -it englishedu-mariadb-1 mariadb -u moodle_user -p moodle_db
# (nhập password từ .env.prod biến MARIADB_PASSWORD)
```

```sql
-- Kiểm tra service ID
SELECT id, shortname FROM mdl_external_services WHERE shortname = 'englishedu_backend';

-- Thêm function (thay X bằng ID tìm được ở trên, thường là 1)
INSERT INTO mdl_external_services_functions (externalserviceid, functionname)
SELECT id, 'enrol_manual_unenrol_users'
FROM mdl_external_services
WHERE shortname = 'englishedu_backend';

-- Xác nhận danh sách function
SELECT functionname FROM mdl_external_services_functions
WHERE externalserviceid = (SELECT id FROM mdl_external_services WHERE shortname = 'englishedu_backend')
ORDER BY functionname;
```

> **Lưu ý:** Không cần restart Moodle sau khi thêm function.

#### ② Rebuild và restart backend

```bash
cd /path/to/EnglishEdu
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod build --no-cache backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
```

#### ③ Chạy sync đầu tiên (1 lần)

Sau khi backend up, gọi các endpoint sau theo thứ tự:

```bash
# 1. Sync tất cả users lên Moodle (đảm bảo mọi user có moodleId)
curl -X POST http://localhost/api/v1/admin/moodle/sync-users \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# 2. Import toàn bộ courses từ Moodle về local DB
curl -X POST http://localhost/api/v1/admin/moodle/import-courses \
  -H "Authorization: Bearer <ADMIN_TOKEN>"

# 3. Sync toàn bộ enrollment từ Moodle cho mọi student
curl -X POST http://localhost/api/v1/admin/moodle/sync-enrollments \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

> Để lấy `ADMIN_TOKEN`, đăng nhập qua:
> ```bash
> curl -X POST http://localhost/api/v1/auth/login \
>   -H "Content-Type: application/json" \
>   -d '{"username":"admin","password":"<password>"}'
> ```

---

## Các vấn đề đã fix trong session này (chưa merge link changelog cũ)

| # | Vấn đề | Nguyên nhân | Fix | File |
|---|--------|-------------|-----|------|
| 1 | MariaDB unhealthy | Sai healthcheck path | `mysqladmin ping` | `docker-compose.prod.yml` |
| 2 | Moodle redirect về login | `wwwroot` thiếu `:8080` | Patch `config.php` | server |
| 3 | Backend 500 `/actuator/health` | Thiếu dependency actuator | Thêm vào `pom.xml` | `pom.xml` |
| 4 | `user-token` lỗi "User not found" | `findByUsername` dùng với numeric ID | Đổi sang `findById` | `MoodleController.java` |
| 5 | Moodle REST API trả HTML | `MOODLE_URL=http://moodle:8080` không khớp wwwroot | Đổi thành `http://${LMS_DOMAIN}:8080` | `docker-compose.prod.yml` |
| 6 | `sync-users` trả 0 | API function sai (`core_user_get_users`) | Đổi sang `core_user_get_users_by_field` | `MoodleClient.java` |
| 7 | `invalid_dataroot_permissions` | Sai ownership `/bitnami/moodledata` | `chown -R daemon:daemon` | server |
| 8 | 403 `/api/auth/login` | Sai URL prefix | Dùng `/api/v1/auth/login` | - |
| 9 | Gán user trên Moodle → không vào được web | Không sync enrollment ngược lại | Nhiều fix (xem [Unreleased]) | nhiều file |
| 10 | Revoke enrollment không sync Moodle | Thiếu `unenrolUser` call | Thêm vào `revokeEnrollment()` | `EnrollmentService.java` |
| 11 | Teacher-launch trả Docker-internal URL | Dùng `getUrl()` thay vì `getPublicUrl()` | Sửa | `MoodleController.java` |

---

## Danh sách Moodle Web Service Functions (service: `englishedu_backend`)

| Function | Dùng cho | Trạng thái |
|----------|----------|-----------|
| `core_webservice_get_site_info` | Test connection | ✅ |
| `core_user_get_users_by_field` | Tìm user theo username | ✅ |
| `core_user_create_users` | Tạo user mới | ✅ |
| `core_user_update_users` | Cập nhật user (reset password) | ✅ |
| `core_enrol_get_users_courses` | Lấy danh sách course của user | ✅ |
| `core_course_get_courses` | Lấy tất cả courses | ✅ |
| `core_course_get_contents` | Lấy nội dung course | ✅ |
| `enrol_manual_enrol_users` | Ghi danh student/teacher | ✅ |
| `auth_userkey_request_login_url` | SSO login URL | ✅ |
| `core_user_get_users` | Tìm user (admin search) | ✅ |
| `enrol_manual_unenrol_users` | Bỏ ghi danh | ⚠️ **Cần thêm thủ công** |

---

## Cấu hình server hiện tại

| Biến | Giá trị |
|------|---------|
| Server IP | `14.225.217.172` |
| Moodle wwwroot | `http://14.225.217.172:8080` |
| Moodle service | `englishedu_backend` |
| Moodle token | `a9451d4adc6bf611b14a917b21cfe35e1bf85551` |
| SSO secret | trong `.env.prod` |
| Backend port | 4000 (internal, qua nginx `:80`) |
| MinIO console | `:9001` |

> **QUAN TRỌNG:** KHÔNG restart Moodle container — Bitnami sẽ ghi đè `config.php` và reset `wwwroot`

---

## Các việc chưa làm

- [x] ~~Fix font encoding `┬á` trong quiz UPPER-IELTS~~ ✅ Script `moddle-lms/fix-font-encoding.sql` đã tạo
- [x] ~~Cập nhật IP trong `DEPLOY-GUIDE.md`~~ ✅ Đã sửa
- [x] ~~Security audit — xóa hardcoded credentials~~ ✅ Đã fix (pom.xml, application.properties, .env.prod.example)
- [x] ~~Payment idempotency~~ ✅ Đã fix
- [x] ~~File upload validation~~ ✅ Đã fix
- [x] ~~JWT/Moodle secret validation~~ ✅ Đã fix
- [x] ~~Docker health checks~~ ✅ Đã thêm (backend, postgres, redis)
- [x] ~~Nginx security headers~~ ✅ Đã thêm
- [x] ~~Rate limiting cho payment endpoints~~ ✅ Đã fix (Redis, 10 req/60s per IP)
- [x] ~~Password policy cho đăng ký và Reset password~~ ✅ Đã fix (min 8 ký tự, cần chữ hoa + chữ thường + số)
- [x] ~~Inactive user vẫn login được~~ ✅ Đã fix (UserDetailsServiceImpl)
- [x] ~~PaymentCallbackRequest không validate input~~ ✅ Đã fix (@NotBlank, @Pattern)
- [x] ~~Moodle error lộ ra client~~ ✅ GlobalExceptionHandler — thông báo generic
- [x] ~~CourseAssignmentService getReferenceById~~ ✅ Đổi sang findById.orElseThrow
- [x] ~~CourseService teacher update silent‐fail~~ ✅ Đổi sang orElseThrow
- [x] ~~Username thuần số cho phép~~ ✅ Regex bắt buộc ít nhất 1 chữ cái
- [x] ~~Test coverage thấp (39 tests)~~ ✅ Thêm 26 tests → tổng **65 tests** (AuthService, PaymentService, ReviewService)
- [ ] Test SSO flow đầu-cuối
- [ ] Cài SSL / HTTPS
- [ ] Cấu hình domain (nếu có)

---

## Đánh giá & Yêu cầu Moodle (Feature Requests)

> Các yêu cầu bên dưới đều là thay đổi **phía Moodle admin** — không cần sửa code Java/TypeScript

| # | Yêu cầu | Loại | Trạng thái | Hướng xử lý |
|---|---------|------|------------|-------------|
| MR-01 | Bỏ cột thông tin câu hỏi trong giao diện chỉnh sửa nội dung khóa học | CSS/Theme | 🔧 Cần làm | CSS ẩn cột qua Additional HTML |
| MR-02 | Sửa lỗi UI Moodle | CSS/Theme | 🔧 Cần làm | Custom CSS qua Additional HTML |
| MR-03 | IELTS mode khi tạo quiz (giao diện IDP, bật/tắt copy-paste, theo dõi màn hình) | Plugin | ❌ Cần phát triển plugin | Viết plugin `mod_quiz` hoặc `local_ieltsmode` |
| MR-04 | Tăng max sections/course lên 100 (hiện max 52) | Config | 🔧 Cần làm | Setting trong Moodle admin |
| MR-05 | Module Audio chạy xuyên sections/page | Plugin | ❌ Cần phát triển plugin | Viết plugin `local_audioplayer` |

### Chi tiết xử lý từng mục — xem `TROUBLESHOOT.md` mục "Moodle Feature Requests"
