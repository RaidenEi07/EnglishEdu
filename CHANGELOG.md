# CHANGELOG — EnglishEdu

> Theo dõi tất cả thay đổi code + server kể từ khi deploy  
> IP server: `14.225.217.172` · Moodle: `http://14.225.217.172:8080`

---

## [Unreleased] — Cần deploy lên server

### Thay đổi code (cần rebuild backend)

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

- [ ] Fix font encoding `┬á` trong quiz UPPER-IELTS (double-encoding từ SQL import)
- [ ] Cập nhật IP trong `DEPLOY-GUIDE.md` (còn `14.225.192.133`, phải đổi thành `14.225.217.172`)
- [ ] Test SSO flow đầu-cuối
- [ ] Cài SSL / HTTPS
- [ ] Cấu hình domain (nếu có)
