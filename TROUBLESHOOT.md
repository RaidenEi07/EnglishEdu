# TROUBLESHOOT — EnglishEdu Server Issues Log

> Ghi lại tất cả lỗi gặp phải trong quá trình deploy lên server Ubuntu 22.04  
> IP server: `221.132.21.13` | Stack: Nginx + Spring Boot 4 + Bitnami Moodle 5 + PostgreSQL + MariaDB + Redis + MinIO

---

## Danh sách lỗi theo thứ tự thời gian

---

### ERR-01 — `docker compose` không nhận lệnh
| | |
|---|---|
| **Lệnh lỗi** | `docker-compose up -d` |
| **Thông báo** | `docker-compose: command not found` |
| **Nguyên nhân** | Docker Compose v1 (standalone binary) không có sẵn; server chỉ có Docker plugin v2 |
| **Fix** | Cài `docker-compose-plugin`: `apt install docker-compose-plugin` |
| **Lệnh đúng** | `docker compose` (không có dấu `-`) |

---

### ERR-02 — `deploy.sh` yêu cầu SSL certificate
| | |
|---|---|
| **Thông báo** | Script kiểm tra cert file trước khi deploy |
| **Nguyên nhân** | Script được viết với giả định có SSL, nhưng server chưa có domain/cert |
| **Fix** | Thêm logic skip SSL check khi chạy HTTP-only mode |

---

### ERR-03 — Nginx lỗi ngay khi start
| | |
|---|---|
| **Thông báo** | `nginx: [emerg] cannot load certificate` |
| **Nguyên nhân** | `nginx.conf` có block `ssl_certificate` nhưng file cert không tồn tại |
| **Fix** | Dùng `nginx/nginx.conf` (HTTP-only version, không có SSL block) |

---

### ERR-04 — Frontend build lỗi `Could not resolve entry module`
| | |
|---|---|
| **Thông báo** | `vite: Could not resolve entry module 'index.html'` |
| **Nguyên nhân** | `index.html` gốc chưa được commit lên GitHub |
| **Fix** | `git add -A && git commit && git push` từ máy Windows |

---

### ERR-05 — MariaDB container `unhealthy`
| | |
|---|---|
| **Thông báo** | `mariadb  ... (unhealthy)` trong `docker compose ps` |
| **Nguyên nhân** | Healthcheck dùng path sai: `/opt/bitnami/mariadb/bin/mysqladmin` không tồn tại trong image version mới |
| **Fix** | Đổi healthcheck trong `docker-compose.prod.yml`: |
```yaml
healthcheck:
  test: ["CMD-SHELL", "/opt/bitnami/mariadb/bin/mysqladmin ping -h 127.0.0.1 -u root 2>/dev/null || /opt/bitnami/mariadb/bin/mariadb-admin ping -h 127.0.0.1 -u root 2>/dev/null"]
```

---

### ERR-06 — Moodle hiển thị "New Site" (DB trống)
| | |
|---|---|
| **Triệu chứng** | Vào `http://221.132.21.13:8080` thấy Moodle fresh install wizard |
| **Nguyên nhân** | `docker-entrypoint-initdb.d` không chạy với Bitnami MariaDB image |
| **Fix** | Import DB thủ công: |
```bash
docker exec -i englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle < moodle-db-export.sql
```

---

### ERR-07 — Import SQL lỗi `ASCII '\0'` (null bytes)
| | |
|---|---|
| **Thông báo** | `ERROR: ASCII '\0' appeared in the statement` |
| **Nguyên nhân** | PowerShell dùng toán tử `>` xuất file UTF-16 LE (mỗi character thêm byte `0x00`) |
| **Fix** | Re-export SQL từ Moodle với `--hex-blob` và encoding UTF-8 không BOM |

---

### ERR-08 — Moodle `500 Internal Server Error` sau import DB
| | |
|---|---|
| **Thông báo** | HTTP 500 khi truy cập bất kỳ URL Moodle nào |
| **Nguyên nhân** | `wwwroot` trong DB (và `config.php`) vẫn là `127.0.0.1:8080` thay vì IP công khai |
| **Fix** | Patch `config.php` theo cách copy-out/edit/copy-in (KHÔNG restart container): |
```bash
docker cp englishedu-moodle-1:/bitnami/moodle/config.php ./config.php
sed -i "s|http://127.0.0.1:8080|http://221.132.21.13:8080|g" config.php
docker cp ./config.php englishedu-moodle-1:/bitnami/moodle/config.php
```
> ⚠️ **KHÔNG restart Moodle container** — Bitnami ghi đè `config.php` khi restart

---

### ERR-09 — Backend `500` trên `/actuator/health`
| | |
|---|---|
| **Thông báo** | `500 Internal Server Error` khi curl `/actuator/health` |
| **Nguyên nhân** | Thiếu dependency `spring-boot-starter-actuator` trong `pom.xml` |
| **Fix** | Thêm vào `pom.xml` + cấu hình trong `application.properties`: |
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

---

### ERR-10 — `/api/v1/moodle/user-token` lỗi "User not found"
| | |
|---|---|
| **Nguyên nhân** | `principal.getUsername()` trả về **numeric ID** (vì JWT lưu ID), nhưng code gọi `findByUsername()` |
| **Fix** | Đổi thành `findById(Long.parseLong(principal.getUsername()))` trong `MoodleController.java` |

---

### ERR-11 — Moodle REST API trả về HTML thay vì JSON
| | |
|---|---|
| **Triệu chứng** | `sync-users` trả về `0`; log backend: `Moodle returned HTML instead of JSON` |
| **Nguyên nhân gốc (ROOT CAUSE)** | `MOODLE_URL=http://moodle:8080` (Docker internal hostname) không khớp với `wwwroot=http://221.132.21.13:8080`. Moodle **redirect tất cả request** có hostname không khớp wwwroot |
| **Fix** | Đổi trong `docker-compose.prod.yml`: |
```yaml
MOODLE_URL: http://${LMS_DOMAIN}:8080
MOODLE_PUBLIC_URL: http://${LMS_DOMAIN}:8080
```

---

### ERR-12 — `core_user_get_users` bị từ chối
| | |
|---|---|
| **Thông báo** | API error: function not found hoặc HTML response |
| **Nguyên nhân** | Backend dùng function `core_user_get_users` nhưng function này không có trong service `englishedu_backend` với đủ params |
| **Fix** | Đổi sang `core_user_get_users_by_field` với params `field` + `values[0]` trong `MoodleClient.java` |

---

### ERR-13 — `invalid_dataroot_permissions` khi gọi Moodle API
| | |
|---|---|
| **Thông báo** | `Exception - invalid_dataroot_permissions` |
| **Nguyên nhân** | `/bitnami/moodledata` có ownership sai sau khi volume được mount |
| **Fix** | |
```bash
docker exec englishedu-moodle-1 chown -R daemon:daemon /bitnami/moodledata
docker exec englishedu-moodle-1 chmod -R 775 /bitnami/moodledata
```

---

### ERR-14 — `403 Forbidden` khi POST `/api/auth/login`
| | |
|---|---|
| **Nguyên nhân** | Sai prefix URL. SecurityConfig chỉ permit `/api/v1/auth/**` |
| **Fix** | Dùng đúng URL: `POST /api/v1/auth/login` |

---

### ERR-15 — `curl localhost:4000` không có phản hồi
| | |
|---|---|
| **Nguyên nhân** | Backend dùng `expose: 4000` (chỉ nội bộ Docker network), không `ports` → không accessible từ host |
| **Fix** | Chỉ gọi backend qua Nginx port 80: `curl http://localhost/api/v1/...` |

---

### ERR-16 — Font bị lỗi `┬á` trong nội dung quiz (UPPER-IELTS)
| | |
|---|---|
| **Triệu chứng** | Ký tự `┬á` xuất hiện trong text, thay vì dấu cách hoặc ký tự đặc biệt |
| **Nguyên nhân** | Double-encoding UTF-8 khi import SQL (file SQL encode UTF-8, import lại encode thêm lần nữa) |
| **Fix** | ❌ Chưa xử lý — cần fix SQL export/import với charset đúng |

---

### ERR-17 — Gán user vào course trên Moodle nhưng web không thể vào course
| | |
|---|---|
| **Triệu chứng** | Admin gán student vào course trên Moodle UI, nhưng trên website EnglishEdu student không thấy/không vào được |
| **Nguyên nhân** | 3 lý do cộng dồn: |
| | 1. `syncMoodleEnrollmentsToLocal()` skip nếu user chưa có `moodleId` |
| | 2. Course chỉ tồn tại trên Moodle (chưa có trong DB local) → `findByMoodleCourseId()` trả empty |
| | 3. Không có endpoint admin để bulk-sync enrollment |
| **Fix** | Xem chi tiết trong `CHANGELOG.md` mục `[Unreleased]` |

---

### ERR-18 — Không đăng nhập được vào Moodle (http://221.132.21.13:8080/login)
| | |
|---|---|
| **Triệu chứng** | Nhập `admin / Admin@123` nhưng không vào được |
| **Nguyên nhân có thể** | Xem mục debug bên dưới |
| **Fix** | Xem mục **"Debug ERR-18"** bên dưới |

---

### ERR-19 — 502 Bad Gateway trên tất cả API
| | |
|---|---|
| **Triệu chứng** | Mọi request `/api/...` trả về `502 Bad Gateway` từ nginx/1.25.5 |
| **Nguyên nhân gốc** | Backend container crash khi khởi tạo bean `MoodleClient` — constructor gọi `RestClient.builder().baseUrl(props.getUrl())` mà `MOODLE_URL` chưa được set hoặc null → `IllegalArgumentException` → Spring Boot fail to start → container exit |
| **Fix** |
| | 1. Đổi `RestClient` sang lazy-init (chỉ tạo khi cần, không tạo trong constructor) |
| | 2. Đổi `MoodleProperties` annotation từ `@Configuration` → `@Component` |
| | 3. Đảm bảo `.env.prod` trên server có `LMS_DOMAIN=221.132.21.13` |
| **File sửa** | `MoodleClient.java`, `MoodleProperties.java` |

---

### ERR-20 — Payment webhook replay dẫn đến duplicate enrollment activation
| | |
|---|---|
| **Triệu chứng** | Webhook từ payment gateway gọi lại 2 lần → enrollment kích hoạt 2 lần, notification gửi 2 lần |
| **Nguyên nhân** | `handlePaymentCallback()` không kiểm tra payment đã ở trạng thái terminal (`COMPLETED`/`FAILED`) |
| **Fix** | Thêm idempotency guard: nếu `payment.getStatus()` đã là `COMPLETED` hoặc `FAILED` → return ngay |
| **File sửa** | `PaymentService.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-21 — Upload file không giới hạn loại & kích thước
| | |
|---|---|
| **Triệu chứng** | Có thể upload `.jar`, `.exe`, hoặc file lớn vô giới hạn |
| **Nguyên nhân** | `StorageService.uploadFile()` chỉ lấy extension, không validate whitelist hay size |
| **Fix** | Thêm whitelist extension (jpg, png, pdf, doc, mp3...) + max 10 MB |
| **File sửa** | `StorageService.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-22 — Hardcoded credentials trong source code
| | |
|---|---|
| **Triệu chứng** | `git log` hiển thị password DB `GilArchon` trong `pom.xml` và `application.properties` |
| **Nguyên nhân** | Flyway Maven plugin hardcode URL/user/password. DataSource default cũng để password |
| **Fix** | Đổi sang env vars: `${env.DB_URL}`, `${env.DB_USERNAME}`, `${env.DB_PASSWORD}`. Xóa default password |
| **File sửa** | `pom.xml`, `application.properties` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-23 — JWT_SECRET và MOODLE_SSO_SECRET default không an toàn
| | |
|---|---|
| **Triệu chứng** | App start thành công với secret mặc định → JWT có thể bị forge |
| **Nguyên nhân** | `application.properties` có default cho `jwt.secret`, `moodle.sso-secret` |
| **Fix** | Xóa default. `JwtProperties` fail-fast nếu secret < 32 chars. `MoodleProperties` warn nếu dùng default |
| **File sửa** | `JwtProperties.java`, `MoodleProperties.java`, `application.properties` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-24 — .env.prod.example chứa credentials thật
| | |
|---|---|
| **Triệu chứng** | Email thật, password thật, JWT secret thật committed vào git |
| **Nguyên nhân** | File example dùng giá trị sản xuất thay vì placeholder |
| **Fix** | Thay toàn bộ bằng `CHANGE_ME_*` placeholder |
| **File sửa** | `.env.prod.example` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-25 — Email gửi thất bại trả 500 không rõ ràng
| | |
|---|---|
| **Triệu chứng** | Forgot password → 500 Internal Server Error (mail server unreachable) |
| **Nguyên nhân** | `EmailService.sendPasswordResetEmail()` không bắt `MailException` |
| **Fix** | try/catch với log + thông báo tiếng Việt: "Không thể gửi email..." |
| **File sửa** | `EmailService.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-26 — NotificationPushService crash khi user đã bị xóa
| | |
|---|---|
| **Triệu chứng** | `LazyInitializationException` khi gửi notification cho user không tồn tại |
| **Nguyên nhân** | `getReferenceById()` trả proxy → nổ khi access thuộc tính |
| **Fix** | Đổi sang `findById()` + return sớm nếu user == null |
| **File sửa** | `NotificationPushService.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-27 — Password policy quá yếu
| | |
|---|---|
| **Triệu chứng** | User có thể đăng ký với password `abc123` hoặc `12345678` |
| **Nguyên nhân** | `RegisterRequest` chỉ validate `@Size(min=6)`, không kiểm tra độ phức tạp |
| **Fix** | `@Size(min=8, max=72)` + `@Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")` — bắt buộc chữ hoa, chữ thường, số |
| **File sửa** | `RegisterRequest.java`, `ResetPasswordRequest.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-28 — User bị vô hiệu hóa vẫn đăng nhập được
| | |
|---|---|
| **Triệu chứng** | Admin set `active = false` nhưng user vẫn lấy được JWT và truy cập API |
| **Nguyên nhân** | `UserDetailsServiceImpl` hardcode `true, true, true, true` — bỏ qua field `isActive` |
| **Fix** | `enabled` và `accountNonLocked` được trả về từ `user.isActive()` thay vì `true` |
| **File sửa** | `UserDetailsServiceImpl.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-29 — PaymentCallbackRequest không validate input
| | |
|---|---|
| **Triệu chứng** | Endpoint `/callback` chấp nhận status tùy ý (ví dụ `"HACKED"`) và xử lý như bình thường |
| **Nguyên nhân** | `PaymentCallbackRequest` không có constraint nào trên các field |
| **Fix** | Thêm `@NotBlank` trên `transactionId`, `status`; `@Pattern(regexp="^(COMPLETED\|FAILED)$")` trên `status` |
| **File sửa** | `PaymentCallbackRequest.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-30 — Không có rate limiting cho payment endpoints
| | |
|---|---|
| **Triệu chứng** | Attacker có thể spam `/initiate` hoặc `/callback` hàng nghìn lần/phút |
| **Nguyên nhân** | Không có throttling trên payment controller |
| **Fix** | Tạo `PaymentRateLimitService` dùng Redis counter — max **10 requests/60s** per IP. Trả về `429 Too Many Requests` khi vượt ngưỡng |
| **File sửa** | `PaymentRateLimitService.java` (mới), `PaymentController.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-31 — Moodle API error lộ chi tiết nội bộ ra client
| | |
|---|---|
| **Triệu chứng** | Response body chứa `"Moodle error: Exception - ..."` hoặc URL nội bộ |
| **Nguyên nhân** | `GlobalExceptionHandler.handleMoodleApi()` trả `"Moodle error: " + ex.getMessage()` |
| **Fix** | Trả về thông báo generic: `"Không thể kết nối đến hệ thống LMS. Vui lòng thử lại sau."` |
| **File sửa** | `GlobalExceptionHandler.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-32 — CourseAssignmentService crash khi adminId không tồn tại
| | |
|---|---|
| **Triệu chứng** | `LazyInitializationException` hoặc EntityNotFoundException ở tầng persistence khi admin user bị xóa |
| **Nguyên nhân** | `userRepository.getReferenceById(adminId)` trả JPA proxy; lỗi không xảy ra ngay lập tức mà khi JPA access entity |
| **Fix** | Đổi sang `findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Admin user not found"))` |
| **File sửa** | `CourseAssignmentService.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-33 — Update course teacher thất bại âm thầm
| | |
|---|---|
| **Triệu chứng** | Admin gửi `teacherId=999` (không tồn tại) → API trả 200 OK nhưng teacher không cập nhật |
| **Nguyên nhân** | `userRepository.findById(teacherId).ifPresent(...)` → nếu teacher không tìm thấy, `ifPresent` skip không throw |
| **Fix** | Đổi sang `.orElseThrow(() -> new ResourceNotFoundException("Teacher not found"))` |
| **File sửa** | `CourseService.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-34 — Username là số thuần túy
| | |
|---|---|
| **Triệu chứng** | User đăng ký username `"12345"` — trùng với pattern ID số trong controller (`Long.parseLong`) |
| **Nguyên nhân** | Regex `^[a-zA-Z0-9_.-]+$` cho phép username toàn số |
| **Fix** | Đổi regex thành `^(?=.*[a-zA-Z])[a-zA-Z0-9_.-]+$` — bắt buộc ít nhất 1 chữ cái |
| **File sửa** | `RegisterRequest.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-35 — Moodle API `invalidtoken` do POST body bị mất khi redirect
| | |
|---|---|
| **Triệu chứng** | Mọi Moodle API call (sync-users, enroll, get courses) trả về `invalidtoken` hoặc `accessexception` |
| **Nguyên nhân gốc** | Backend POST tới `http://moodle:8080` → Moodle redirect (303) tới `http://221.132.21.13:8080` (do wwwroot ≠ hostname). `HttpClient.Redirect.NORMAL` chuyển POST thành GET (RFC 7231) → body chứa `wstoken` bị mất → Moodle trả `invalidtoken` |
| **Fix** | Đổi `followRedirects(NORMAL)` → `followRedirects(NEVER)`. Thêm manual re-POST đến `Location` URL với cùng body gốc |
| **File sửa** | `MoodleClient.java` |
| **Trạng thái** | ✅ Đã fix |

---

### ERR-36 — Student login thành công nhưng ngay lập tức lỗi 500 trên `/courses/enrolled`
| | |
|---|---|
| **Triệu chứng** | Student đăng nhập → frontend gọi `GET /api/v1/courses/enrolled` → 500 Internal Server Error |
| **Nguyên nhân gốc** | 3 nguyên nhân cộng dồn: |
| | 1. `SecurityConfig`: `permitAll()` cho `/courses/{id}` vô tình match cả `/courses/enrolled` → request đi vào endpoint mà không cần auth |
| | 2. `@AuthenticationPrincipal UserDetails user` = null → `getUserId(null)` → NPE |
| | 3. `MoodleSyncService.provisionMoodleUser()`: retry lookup trong catch block throw uncaught exception |
| **Fix** | |
| | 1. SecurityConfig: Thêm explicit `.authenticated()` cho `/courses/enrolled`, `/courses/assigned`, `/courses/recent`, `/courses/dashboard` trước `{id}`. Đổi `{id}` → `{id:\\d+}` |
| | 2. Controller: Thêm null check trong `getUserId()` → throw `BadRequestException` |
| | 3. MoodleSyncService: Wrap retry lookup trong try/catch riêng |
| **File sửa** | `SecurityConfig.java`, `CourseController.java`, `UserController.java`, `MoodleSyncService.java` |
| **Trạng thái** | ✅ Đã fix |

---

## Debug ERR-18 — Không đăng nhập được Moodle

### Bước 1: Kiểm tra wwwroot trong config.php

```bash
docker exec englishedu-moodle-1 grep "wwwroot" /bitnami/moodle/config.php
```

Kết quả phải là:
```
$CFG->wwwroot   = 'http://221.132.21.13:8080';
```

Nếu SAI (ví dụ còn `http://moodle:8080` hoặc IP cũ) → patch lại:
```bash
docker cp englishedu-moodle-1:/bitnami/moodle/config.php ./config.php
sed -i "s|http://moodle:8080|http://221.132.21.13:8080|g" config.php
sed -i "s|http://127.0.0.1:8080|http://221.132.21.13:8080|g" config.php
sed -i "s|http://221.132.21.13:8080|http://221.132.21.13:8080|g" config.php
docker cp ./config.php englishedu-moodle-1:/bitnami/moodle/config.php
```

---

### Bước 2: Reset password admin qua Moodle CLI

Đây là cách **chắc chắn nhất**, không phụ thuộc DB hay env var:

```bash
docker exec -it englishedu-moodle-1 php /opt/bitnami/moodle/admin/cli/reset_password.php \
  --username=admin \
  --password=Admin@123
```

Nếu path trên không đúng, thử:
```bash
docker exec englishedu-moodle-1 find / -name "reset_password.php" 2>/dev/null
```

---

### Bước 3: Kiểm tra tài khoản admin trong DB

```bash
docker exec -it englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle \
  -e "SELECT id, username, email, deleted, suspended FROM mdl_user WHERE username='admin';"
```

- Nếu `deleted=1` → tài khoản bị xóa → cần restore
- Nếu `suspended=1` → tài khoản bị suspend → cần bỏ suspend

Fix nếu bị suspended:
```sql
UPDATE mdl_user SET suspended=0, deleted=0 WHERE username='admin';
```

---

### Bước 4: Xóa session cache

```bash
docker exec -it englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle \
  -e "DELETE FROM mdl_sessions;"
```

Sau đó thử đăng nhập lại.

---

## Quick Reference — Lệnh hay dùng trên server

```bash
# Xem status các container
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

# Xem log backend
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f --tail=50 backend

# Xem log moodle
docker compose -f docker-compose.prod.yml --env-file .env.prod logs -f --tail=50 moodle

# Restart backend (an toàn)
docker compose -f docker-compose.prod.yml --env-file .env.prod restart backend

# ⚠️ TUYỆT ĐỐI KHÔNG restart moodle — sẽ ghi đè config.php
# Nếu bắt buộc phải restart moodle, phải patch lại config.php sau đó

# Vào DB MariaDB (Moodle)
docker exec -it englishedu-mariadb-1 mariadb -u bn_moodle -pbn_pass bitnami_moodle

# Vào DB PostgreSQL (Backend)
docker exec -it englishedu-postgres-1 psql -U sso_user -d sso_db

# Rebuild backend sau khi thay đổi code
docker compose -f docker-compose.prod.yml --env-file .env.prod build --no-cache backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend

# Test Moodle API connection
curl -X POST http://localhost/api/v1/admin/moodle/status \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

## Cấu hình quan trọng đã thay đổi so với default

| File | Thay đổi | Lý do |
|------|----------|-------|
| `docker-compose.prod.yml` | `MOODLE_URL` → `http://${LMS_DOMAIN}:8080` | Phải khớp `wwwroot` |
| `docker-compose.prod.yml` | `MOODLE_HTTP_PORT_NUMBER=8080` | Bitnami tự set port |
| `docker-compose.prod.yml` | MariaDB healthcheck dùng `mysqladmin ping` | Path đúng với image |
| `docker-compose.prod.yml` | Health checks cho backend, postgres, redis | Phát hiện crash sớm |
| `docker-compose.prod.yml` | Backend `depends_on` condition `service_healthy` | Đợi DB/Redis ready |
| `application.properties` | `moodle.public-url` thêm | SSO URL cho browser |
| `application.properties` | Xóa default credentials (DB, JWT, MinIO) | Bảo mật |
| `pom.xml` | Flyway plugin dùng env vars thay cho hardcoded creds | Bảo mật |
| `nginx.conf` | Security headers + `client_max_body_size 10m` | Hardening |
| `MoodleClient.java` | HTML detection + timeout 30s | Phát hiện sớm lỗi redirect |
| `MoodleSyncService.java` | `getPublicUrl()` cho SSO/course URL | URL browser-facing |
| `PaymentService.java` | Idempotency guard | Chống webhook replay |
| `StorageService.java` | File type whitelist + max size 10MB | Bảo mật upload |
| `JwtProperties.java` | Fail-fast nếu JWT_SECRET < 32 chars | Bảo mật |
| `.env.prod.example` | Placeholder thay vì creds thật | Bảo mật |

---

## Moodle Feature Requests — Hướng dẫn chi tiết

> Tất cả các mục dưới đây là thay đổi **phía Moodle** (admin settings, CSS/JS injection, hoặc plugin).  
> Không cần sửa code Java backend hay TypeScript frontend.

---

### MR-01 — Bỏ cột "Thông tin câu hỏi" trong giao diện chỉnh sửa nội dung khóa học

| | |
|---|---|
| **Loại** | CSS injection (đơn giản) |
| **Nơi sửa** | Site Administration → Appearance → Additional HTML → "Within HEAD" |

**Cách làm:**

Thêm CSS sau vào phần `<style>` đã có trong `moodle-customization.html`:

```css
/* MR-01: Ẩn cột thông tin câu hỏi khi chỉnh sửa nội dung khóa học */
.course-content .activity .activity-info,
.course-content .activity .activity-altcontent,
.editing .activity-item .activity-info {
  display: none !important;
}
```

> **Lưu ý:** Cần kiểm tra trên Moodle phiên bản 5.0.1 — class name có thể khác tuỳ theme. Bật chế độ editing, dùng DevTools (F12) → Inspect cột cần ẩn → xác định đúng CSS selector, rồi sửa lại cho chính xác.

**Cách debug nếu không hiệu quả:**
1. Mở Moodle → vào một course → bật "Turn editing on"
2. F12 → Inspect phần tử cột "Thông tin câu hỏi"
3. Lấy CSS class/selector chính xác
4. Cập nhật CSS selector ở trên cho khớp

---

### MR-02 — Sửa lỗi UI Moodle (chung)

| | |
|---|---|
| **Loại** | CSS injection |
| **Nơi sửa** | Site Administration → Appearance → Additional HTML → "Within HEAD" |

**Cần xác định cụ thể:** Yêu cầu này cần thông tin chi tiết hơn:
- Lỗi UI cụ thể là gì? (font bị vỡ, alignment sai, responsive hỏng, màu sắc…)
- Ở trang nào? (dashboard, course view, quiz, …)
- Screenshot nếu có

**Cách xử lý chung:**

1. Mở trang bị lỗi → F12 DevTools → xác định element
2. Viết CSS fix → test trước trong DevTools
3. Thêm vào `moodle-customization.html` phần `<style>` trong "Within HEAD"
4. Vào Moodle Admin → Appearance → Additional HTML → paste → Save

**Ví dụ CSS fixes phổ biến:**

```css
/* Fix font tiếng Việt */
body, .navbar, .card, .activity-item {
  font-family: 'Inter', 'Segoe UI', 'Roboto', sans-serif !important;
}

/* Fix responsive trên mobile */
@media (max-width: 768px) {
  .course-content .section {
    padding: 8px !important;
  }
}
```

---

### MR-03 — IELTS Mode trong Quiz Settings (Giao diện IDP, bật/tắt copy-paste, theo dõi màn hình)

| | |
|---|---|
| **Loại** | **PHỨC TẠP** — Cần plugin + JavaScript injection |
| **Mức độ** | ⚠️ Lớn — ước tính 2-4 tuần dev nếu viết plugin từ đầu |

**Phân tích 3 tính năng con:**

#### 3a. Giao diện dạng IDP (International English Testing interface)

Đây là thay đổi **giao diện quiz** — cần quiz theme override hoặc custom layout.

**Cách 1 — CSS Override (nhanh, giới hạn):**
```css
/* Bố cục quiz dạng IDP — 2 cột */
body.path-mod-quiz #page-content {
  display: flex !important;
  flex-direction: row !important;
}
body.path-mod-quiz #region-main {
  flex: 1;
  max-width: 60%;
}
body.path-mod-quiz .block_region {
  flex: 0 0 40%;
}
```

**Cách 2 — Custom Quiz Layout Plugin (đúng cách):**
- Tạo plugin `local_ieltsmode` hoặc `mod_quiz` renderer override
- Moodle docs: [Quiz renderer override](https://docs.moodle.org/dev/Output_renderers#Overriding_a_renderer)

#### 3b. Bật/tắt Copy-Paste

**Cách nhanh — JavaScript injection (thêm vào "Before BODY is closed"):**

```javascript
// MR-03b: Chặn copy/paste khi quiz đang active
(function() {
  'use strict';
  // Chỉ chạy trên trang quiz attempt
  if (window.location.pathname.indexOf('/mod/quiz/attempt.php') === -1) return;

  // Chặn copy, cut, paste
  document.addEventListener('copy', function(e) { e.preventDefault(); });
  document.addEventListener('cut', function(e) { e.preventDefault(); });
  document.addEventListener('paste', function(e) { e.preventDefault(); });

  // Chặn right-click
  document.addEventListener('contextmenu', function(e) { e.preventDefault(); });

  // Chặn Ctrl+C, Ctrl+V, Ctrl+X
  document.addEventListener('keydown', function(e) {
    if (e.ctrlKey && (e.key === 'c' || e.key === 'v' || e.key === 'x' || e.key === 'a')) {
      e.preventDefault();
    }
  });

  // CSS chặn text selection
  var style = document.createElement('style');
  style.textContent = `
    .que .formulation, .que .answer, .que .qtext {
      -webkit-user-select: none !important;
      -moz-user-select: none !important;
      -ms-user-select: none !important;
      user-select: none !important;
    }
  `;
  document.head.appendChild(style);
})();
```

> **⚠️ Lưu ý:** Client-side chặn copy/paste dễ bị bypass (DevTools). Để nghiêm túc cần kết hợp SEB (Safe Exam Browser) — xem mục 3c.

**Bật/tắt theo quiz:** Cần plugin vì Moodle không có cài đặt copy-paste per-quiz. Hoặc dùng quiz "Browser security" = "Full screen pop-up with some JavaScript security" (có sẵn trong Moodle).

**Cách bật tính năng có sẵn:**
1. Vào quiz → Settings → Extra restrictions on attempts
2. "Browser security" → chọn "Full screen pop-up with some JavaScript security"
3. Moodle sẽ tự chặn copy/paste/right-click + mở fullscreen popup

#### 3c. Theo dõi màn hình (Screen monitoring / Proctoring)

**Đây là tính năng phức tạp nhất.** Có 3 hướng:

| Hướng | Mô tả | Đánh giá |
|-------|-------|----------|
| **Moodle SEB** | Safe Exam Browser — browser khoá | ✅ Có sẵn, cài đặt dễ |
| **Proctoring plugin** | Plugin giám sát webcam/tab | ⚠️ Cần cài plugin bên thứ 3 |
| **Tự viết plugin** | Theo dõi tab focus, ghi log | ⚠️ Dev 1-2 tuần |

**Hướng 1 — Safe Exam Browser (khuyến nghị):**

1. Tải SEB: https://safeexambrowser.org/
2. Cấu hình quiz trong Moodle:
   - Quiz → Settings → Extra restrictions on attempts
   - "Require the use of Safe Exam Browser" → Yes
   - Thêm SEB config key
3. Học sinh phải cài SEB để làm bài

**Hướng 2 — Plugin Proctoring (đơn giản hơn SEB):**

- Plugin: `quizaccess_proctoring` — chụp webcam định kỳ
- Cài qua Moodle Plugin Directory hoặc upload ZIP
- Site Admin → Plugins → Install plugins

**Hướng 3 — JavaScript giám sát tab focus (nhanh):**

```javascript
// MR-03c: Theo dõi khi học sinh chuyển tab
(function() {
  if (window.location.pathname.indexOf('/mod/quiz/attempt.php') === -1) return;

  var switchCount = 0;
  var maxAllowed = 3; // Cho phép tối đa 3 lần chuyển tab

  document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
      switchCount++;
      console.warn('[IELTS Mode] Tab switch #' + switchCount);

      if (switchCount >= maxAllowed) {
        // Tự động nộp bài
        var submitBtn = document.querySelector('input[name="finishattempt"]') ||
                        document.querySelector('.submitbtns button');
        if (submitBtn) {
          alert('Bạn đã chuyển tab quá ' + maxAllowed + ' lần. Bài thi sẽ được nộp tự động.');
          submitBtn.click();
        }
      } else {
        alert('Cảnh báo: Bạn đã chuyển tab ' + switchCount + '/' + maxAllowed + ' lần. Vượt quá giới hạn bài thi sẽ tự nộp.');
      }
    }
  });
})();
```

---

### MR-04 — Tăng Max Sections/Course lên 100 (hiện tại max 52)

| | |
|---|---|
| **Loại** | Moodle admin setting (đơn giản nhất) |
| **Thời gian** | 1 phút |

**Các bước:**

1. Đăng nhập Moodle bằng tài khoản Admin
2. Vào **Site Administration → Plugins → Course formats → Topics format**
3. Tìm mục **"Maximum number of sections"**
4. Đổi từ `52` → `100` (hoặc giá trị mong muốn)
5. Bấm **"Save changes"**

**Nếu dùng format khác (Weekly, Tiles...):**
- Mỗi course format có setting riêng
- Vào **Site Administration → Plugins → Course formats → [tên format]**
- Sửa "Maximum number of sections" tương ứng

**Nếu cần max > 200:**
- Moodle bắt đầu chậm với quá nhiều sections
- Nên cân nhắc chia nhỏ course thay vì tăng quá cao

---

### MR-05 — Module Audio chạy xuyên sections/page

| | |
|---|---|
| **Loại** | JavaScript injection hoặc custom plugin |
| **Mức độ** | ⚠️ Trung bình — JS injection có thể làm nhanh |

**Yêu cầu:** Một audio player cố định (sticky) trên trang, tiếp tục phát khi chuyển section/tab trong cùng course.

**Giải pháp nhanh — Sticky Audio Player (JS injection):**

Thêm vào `moodle-customization.html`:

**CSS (thêm vào "Within HEAD"):**
```css
/* MR-05: Sticky Audio Player */
#sso-audio-player {
  position: fixed;
  bottom: 80px; /* Phía trên nút "Quay lại Dashboard" */
  right: 24px;
  z-index: 99998;
  background: #1a1a2e;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.3);
  display: none; /* Ẩn khi không có audio */
  max-width: 320px;
}
#sso-audio-player audio {
  width: 100%;
  height: 36px;
}
#sso-audio-player .audio-title {
  color: #ccc;
  font-size: 12px;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
#sso-audio-player .audio-close {
  position: absolute;
  top: 4px;
  right: 8px;
  color: #888;
  cursor: pointer;
  font-size: 16px;
  background: none;
  border: none;
}
```

**JavaScript (thêm vào "Before BODY is closed"):**
```javascript
// MR-05: Sticky Audio Player — chạy xuyên sections
(function() {
  'use strict';
  // Chỉ chạy trong course view
  if (window.location.pathname.indexOf('/course/view.php') === -1) return;

  // Tạo container audio player
  var container = document.createElement('div');
  container.id = 'sso-audio-player';
  container.innerHTML = '<button class="audio-close">&times;</button>' +
    '<div class="audio-title"></div>' +
    '<audio controls></audio>';
  document.body.appendChild(container);

  var audio = container.querySelector('audio');
  var titleEl = container.querySelector('.audio-title');
  var closeBtn = container.querySelector('.audio-close');

  // Lưu/khôi phục trạng thái audio qua sessionStorage
  var storageKey = 'sso_audio_' + (new URLSearchParams(window.location.search).get('id') || '0');
  var saved = JSON.parse(sessionStorage.getItem(storageKey) || 'null');

  if (saved && saved.src) {
    audio.src = saved.src;
    audio.currentTime = saved.time || 0;
    titleEl.textContent = saved.title || 'Audio';
    container.style.display = 'block';
    if (saved.playing) audio.play();
  }

  // Lưu trạng thái trước khi rời trang
  window.addEventListener('beforeunload', function() {
    if (audio.src) {
      sessionStorage.setItem(storageKey, JSON.stringify({
        src: audio.src,
        time: audio.currentTime,
        playing: !audio.paused,
        title: titleEl.textContent
      }));
    }
  });

  // Bắt click vào audio resource trong course
  document.addEventListener('click', function(e) {
    var link = e.target.closest('a[href*="/mod/resource/view.php"], a[href*="/pluginfile.php"]');
    if (!link) return;

    var href = link.href;
    // Kiểm tra đuôi file audio
    if (!/\.(mp3|wav|ogg|m4a|aac|flac)/i.test(href)) return;

    e.preventDefault();
    audio.src = href;
    titleEl.textContent = link.textContent.trim() || 'Audio';
    container.style.display = 'block';
    audio.play();
  });

  // Nút đóng
  closeBtn.addEventListener('click', function() {
    audio.pause();
    audio.src = '';
    container.style.display = 'none';
    sessionStorage.removeItem(storageKey);
  });
})();
```

**Hạn chế của giải pháp JS injection:**
- Audio sẽ bị ngắt khi chuyển trang (full page reload) — phải dùng `sessionStorage` để resume
- Chuyển section bằng AJAX (nếu format hỗ trợ) thì audio tiếp tục phát
- Muốn audio không bị ngắt hoàn toàn khi chuyển trang → cần Service Worker hoặc iframe (phức tạp hơn)

**Giải pháp bền vững (khuyến nghị nếu cần chất lượng cao):**
- Viết plugin `local_audioplayer` với audio nằm trong `<iframe>` hoặc popup riêng
- Hoặc dùng AJAX course format (vd: "Tiles") để navigate không reload trang
