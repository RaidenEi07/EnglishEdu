# TROUBLESHOOT — EnglishEdu Server Issues Log

> Ghi lại tất cả lỗi gặp phải trong quá trình deploy lên server Ubuntu 22.04  
> IP server: `14.225.217.172` | Stack: Nginx + Spring Boot 4 + Bitnami Moodle 5 + PostgreSQL + MariaDB + Redis + MinIO

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
| **Triệu chứng** | Vào `http://14.225.217.172:8080` thấy Moodle fresh install wizard |
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
sed -i "s|http://127.0.0.1:8080|http://14.225.217.172:8080|g" config.php
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
| **Nguyên nhân gốc (ROOT CAUSE)** | `MOODLE_URL=http://moodle:8080` (Docker internal hostname) không khớp với `wwwroot=http://14.225.217.172:8080`. Moodle **redirect tất cả request** có hostname không khớp wwwroot |
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

### ERR-18 — Không đăng nhập được vào Moodle (http://14.225.217.172:8080/login)
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
| | 3. Đảm bảo `.env.prod` trên server có `LMS_DOMAIN=14.225.217.172` |
| **File sửa** | `MoodleClient.java`, `MoodleProperties.java` |

---

## Debug ERR-18 — Không đăng nhập được Moodle

### Bước 1: Kiểm tra wwwroot trong config.php

```bash
docker exec englishedu-moodle-1 grep "wwwroot" /bitnami/moodle/config.php
```

Kết quả phải là:
```
$CFG->wwwroot   = 'http://14.225.217.172:8080';
```

Nếu SAI (ví dụ còn `http://moodle:8080` hoặc IP cũ) → patch lại:
```bash
docker cp englishedu-moodle-1:/bitnami/moodle/config.php ./config.php
sed -i "s|http://moodle:8080|http://14.225.217.172:8080|g" config.php
sed -i "s|http://127.0.0.1:8080|http://14.225.217.172:8080|g" config.php
sed -i "s|http://14.225.192.133:8080|http://14.225.217.172:8080|g" config.php
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
| `application.properties` | `moodle.public-url` thêm | SSO URL cho browser |
| `MoodleClient.java` | HTML detection + timeout 30s | Phát hiện sớm lỗi redirect |
| `MoodleSyncService.java` | `getPublicUrl()` cho SSO/course URL | URL browser-facing |
