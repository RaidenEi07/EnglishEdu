# Hướng dẫn Deploy EnglishEdu lên Server

> **Server:** Ubuntu 22.04 · IP: `221.132.21.13` · Chưa có domain · HTTP only

---

## Tổng quan hệ thống

| Service      | Công nghệ              | Port ngoài |
|-------------|------------------------|-----------|
| **Frontend** | Vite + TypeScript      | `:80`     |
| **Backend**  | Spring Boot 4 (Java 17)| qua Nginx `/api/` |
| **Moodle**   | Bitnami Moodle LMS     | `:8080`   |
| **PostgreSQL**| v16 (cho Spring Boot) | internal  |
| **MariaDB**  | Bitnami (cho Moodle)   | internal  |
| **Redis**    | v7 (JWT blacklist)     | internal  |
| **MinIO**    | File storage           | `:9001` (console) |

---

## Các vấn đề đã gặp & đã fix

| # | Vấn đề | Nguyên nhân gốc | Fix |
|---|--------|-----------------|-----|
| 1 | `docker compose` không nhận lệnh | Docker Compose plugin v2 chưa cài | Cài `docker-compose-plugin` |
| 2 | `deploy.sh` yêu cầu SSL | Script check cert cứng | Thêm logic skip SSL khi HTTP mode |
| 3 | Nginx lỗi vì thiếu cert | Config có `ssl_certificate` | Chuyển sang `nginx.conf` HTTP-only |
| 4 | Frontend build lỗi `Could not resolve entry module` | `index.html` chưa push lên GitHub | Commit & push file |
| 5 | Moodle `500 Internal Server Error` | `wwwroot` trong DB = `127.0.0.1:8080` | Update sau khi import DB |
| 6 | Moodle hiện "New Site" (DB trống) | `docker-entrypoint-initdb.d` không chạy với Bitnami image | Import thủ công |
| 7 | Import SQL lỗi `ASCII '\0'` | PowerShell `>` xuất file UTF-16 (mỗi char thêm `0x00`) | Re-export với `--hex-blob` + encoding UTF-8 không BOM |
| 8 | Env vars trống (WARN) | Chạy lệnh thiếu `--env-file .env.prod` | Đúng lệnh |
| 9 | Moodle báo "invalid URL" + CSS không load | Bitnami sinh config.php với PHP concatenation sai: `'http://IP:8080' . 'IP'` → evaluate ra URL vô nghĩa. Sed cũ chỉ replace chuỗi quoted đầu, để lại phần `. 'IP'` | Dùng `sed` replace **toàn bộ dòng** `$CFG->wwwroot` (xem bước 5.1) |
| 10 | Nút "Về trang chủ" trong Moodle trỏ về `localhost:3000` | `additionalhtmlfooter` trong DB export dùng URL dev hardcoded | Chạy UPDATE SQL sửa URL sau khi import (bước 5.4c) |
| 11 | Không cài được `auth_userkey` plugin (container thiếu `curl`, `wget`, `unzip`) | Bitnami Moodle image là minimal — không có network tools | Build custom Dockerfile (`moddle-lms/Dockerfile`) bake plugin vào image, kèm script `fix-wwwroot.sh` tự fix config |

---

## BƯỚC 0 — Chuẩn bị trên máy Windows

### 0.1 Push code mới nhất lên GitHub:

```powershell
cd "c:\My Web Sites\EnglishEdu"
git add -A
git commit -m "Fix: all deployment config for HTTP/IP mode"
git push
```

---

## BƯỚC 1 — Cài đặt server (chạy 1 lần)

```bash
ssh root@221.132.21.13

# Cập nhật OS
apt update && apt upgrade -y

# Cài Docker + Compose plugin
curl -fsSL https://get.docker.com | sh
apt install -y docker-compose-plugin

# Kiểm tra
docker --version          # Docker 24+
docker compose version    # Docker Compose v2+

# Cài Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt install -y nodejs
node --version            # v20+
```

---

## BƯỚC 2 — Clone repo

```bash
cd ~

# Xóa thư mục cũ nếu có
rm -rf ~/EnglishEdu

git clone https://github.com/<user>/<repo>.git EnglishEdu
cd ~/EnglishEdu

# Kiểm tra
ls
# Phải thấy: EnglishEdu_BE  sunshine-rebuild  docker-compose.prod.yml
#             nginx  moddle-lms  .env.prod.example  deploy.sh
```

---

## BƯỚC 3 — Tạo file .env.prod

```bash
cp .env.prod.example .env.prod

# Tạo MOODLE_SSO_SECRET khác JWT_SECRET
openssl rand -hex 32

nano .env.prod
```

Nội dung `.env.prod` (thay `MOODLE_SSO_SECRET` bằng kết quả openssl):

```env
DOMAIN=221.132.21.13
LMS_DOMAIN=221.132.21.13
DB_PASSWORD=Admin@123
DB_NAME=sunshine_db
ADMIN_USERNAME=admin
ADMIN_EMAIL=thinhdoquy@gmail.com
ADMIN_PASSWORD=Admin@123
JWT_SECRET=69fedc8764d5faed2def109787e72de91456d79f94a8c43008bc0144046aaa73
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=Admin@123
JPA_DDL_AUTO=validate
MOODLE_TOKEN=
MOODLE_SSO_SECRET=<kết quả openssl rand -hex 32>
MOODLE_ADMIN_USER=admin
MOODLE_ADMIN_PASSWORD=Admin@123
MOODLE_ADMIN_EMAIL=gilgamesharch2607@gmail.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=thinhdoquy@gmail.com
MAIL_PASSWORD=dieaxhbhptegnivu
SEED_DEMO_TEACHER=false
```

Lưu: `Ctrl+X` → `Y` → `Enter`

---

## BƯỚC 4 — Build frontend + Deploy Docker

```bash
cd ~/EnglishEdu/sunshine-rebuild

# Tạo .env.production cho Vite
cp .env.production.example .env.production

npm install
npm run build

# Kiểm tra
ls dist/    # Phải thấy: index.html, assets/

cd ~/EnglishEdu

# Deploy tất cả services
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# Theo dõi (Ctrl+C để thoát)
docker compose -f docker-compose.prod.yml logs -f
```

**Chờ đến khi thấy tất cả services "started"** (mất 10-20 phút lần đầu).

Kiểm tra:
```bash
docker compose -f docker-compose.prod.yml ps
# Tất cả STATUS phải là "Up"
```

---

## BƯỚC 5 — Kiểm tra & Fix Moodle (fresh DB — Bitnami tự khởi tạo)

> **Không cần import SQL.** Bitnami tự tạo DB mới từ env vars trong `docker-compose.prod.yml`.
> Tài khoản admin: `MOODLE_USERNAME` / `MOODLE_PASSWORD`
>
> ⚠️ **Moodle mất 5-10 phút để khởi tạo DB lần đầu** — chờ trước khi chạy các lệnh dưới.

```bash
# Theo dõi Moodle đã xong chưa:
docker compose -f docker-compose.prod.yml logs -f moodle
# Chờ thấy: "moodle | INFO  ==> Starting Moodle..." hoặc Apache started
# Ctrl+C để thoát

# 5.1 — Kiểm tra wwwroot trong config.php (QUAN TRỌNG — nguyên nhân CSS không load & invalid URL)
docker compose -f docker-compose.prod.yml exec moodle grep "wwwroot" /bitnami/moodle/config.php
# Kết quả ĐÚNG phải là MỘT dòng duy nhất: $CFG->wwwroot   = 'http://<IP>:8080';
#
# LỖI THƯỜNG GẶP — Bitnami sinh ra PHP concatenation sai dạng:
#   $CFG->wwwroot = 'http://IP:8080' . 'IP';   ← invalid URL khi evaluate!
# Nguyên nhân: sed chỉ replace chuỗi quoted đầu tiên, để lại phần '. IP' phía sau.
#
# FIX: replace TOÀN BỘ dòng wwwroot (thay IP bằng IP thực của server):
docker compose -f docker-compose.prod.yml exec moodle bash -c \
  "sed -i \"s|\\\$CFG->wwwroot.*|\\\$CFG->wwwroot   = 'http://221.132.21.13:8080';|\" /bitnami/moodle/config.php && grep wwwroot /bitnami/moodle/config.php"
# Sau khi chạy, grep phải cho đúng 1 dòng: $CFG->wwwroot   = 'http://221.132.21.13:8080';

# 5.2 — Xóa cache + restart để Moodle nhận config mới
docker compose -f docker-compose.prod.yml exec moodle bash -c \
  "rm -rf /bitnami/moodledata/cache/* /bitnami/moodledata/localcache/* /bitnami/moodledata/temp/*"

docker compose -f docker-compose.prod.yml restart moodle
```

**Kiểm tra:** Truy cập `http://221.132.21.13:8080` → CSS phải load đúng, trang không bị vỡ layout.

### Cấu hình Moodle sau khi cài mới

Sau khi Moodle chạy ổn, cần thiết lập thủ công:

**A. Enable auth_userkey plugin trước (BẮT BUỘC làm trước bước B):**
> ✅ Plugin `auth_userkey` đã được bake sẵn trong Docker image (`moddle-lms/Dockerfile`).
> Không cần cài thủ công — chỉ cần bật trong UI.
1. Đăng nhập: `http://<IP>:8080` với `admin` / mật khẩu trong `.env.prod`
2. Nếu Moodle hiện trang **Notifications** với "Database upgrade" → click **Upgrade Moodle database now**
3. **Site Administration → Plugins → Authentication → Manage authentication** → bật **User key authentication**
4. Cấu hình: mapping field = `email`, key lifetime = `60`

**B. Enable Web Services (để backend gọi Moodle API):**
1. **Site Administration → Advanced features** → bật **Enable web services** → Save
2. **Site Administration → Plugins → Web services → Manage protocols** → bật **REST protocol** → Save  
   ⚠️ **Nếu bỏ qua bước này, Moodle trả về empty response → sync-users sẽ fail**
3. **Site Administration → Plugins → Web services → External services** → thêm service mới:
   - Name: `englishedu_backend` | Enable: ✓ | Authorised users only: ✓
   - Thêm **từng** function sau (Moodle không hỗ trợ wildcard `*` khi add):
     - `auth_userkey_request_login_url`
     - `core_user_create_users`
     - `core_user_get_users`
     - `core_user_get_users_by_field`
     - `core_user_update_users`
     - `core_course_create_courses`
     - `core_course_get_courses`
     - `core_course_get_courses_by_field`
     - `core_course_get_contents`
     - `core_enrol_get_enrolled_users`
     - `core_enrol_get_users_courses`
     - `enrol_manual_enrol_users`
     - `enrol_manual_unenrol_users`
     - `gradereport_user_get_grade_items`
     - `core_calendar_get_calendar_events`
     - `core_calendar_get_action_events_by_timesort`
     - `core_completion_get_activities_completion_status`
     - `core_completion_update_activity_completion_status_manually`
     - `mod_assign_get_assignments`
     - `mod_assign_get_submission_status`
     - `mod_assign_save_submission`
     - `mod_assign_submit_for_grading`
     - `mod_quiz_get_quizzes_by_courses`
     - `mod_quiz_get_user_quiz_attempts`
     - `mod_quiz_start_attempt`
     - `mod_quiz_get_attempt_data`
     - `mod_quiz_save_attempt`
     - `mod_quiz_process_attempt`
     - `mod_quiz_get_attempt_review`
     - `mod_quiz_get_attempt_summary`
     - `mod_page_get_pages_by_courses`
     - `mod_resource_get_resources_by_courses`
     - `mod_url_get_urls_by_courses`
4. **Site Administration → Plugins → Web services → Manage tokens** → tạo token cho `admin` với service trên

**C. Áp dụng customization (nút "Quay lại Dashboard"):**
1. **Site Administration → Appearance → Additional HTML**
2. **Within HEAD**: dán nội dung phần 1 từ file `moddle-lms/moodle-customization.html`
3. **Before BODY is closed**: dán nội dung phần 2 từ file `moddle-lms/moodle-customization.html`
4. Save changes

---

## BƯỚC 6 — Lấy Moodle Token

1. Mở `http://221.132.21.13:8080`
2. Đăng nhập: `admin` / `Admin@123`
3. Vào: **Site Administration → Server → Web services → Manage tokens**
4. Copy token

Cập nhật trên server:
```bash
nano ~/EnglishEdu/.env.prod
# Tìm: MOODLE_TOKEN=
# Sửa: MOODLE_TOKEN=<token vừa copy>

# Restart backend
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d backend
```

---

## BƯỚC 7 — Kiểm tra hoàn chỉnh

```bash
# Backend health
curl http://localhost:4000/actuator/health
# → {"status":"UP"}

# Frontend
curl -I http://221.132.21.13
# → HTTP/1.1 200 OK
```

Truy cập trình duyệt:
- **Website:** http://221.132.21.13
- **Moodle:** http://221.132.21.13:8080
- **MinIO Console:** http://221.132.21.13:9001

---

## BƯỚC 8 — Mở Firewall

```bash
ufw allow 22      # SSH (KHÔNG ĐƯỢC QUÊN!)
ufw allow 80      # Frontend
ufw allow 8080    # Moodle
ufw allow 9001    # MinIO Console (tùy chọn)
ufw enable
ufw status
```

---

## Cập nhật code sau này (Khởi động lại sau khi pull)

> Thực hiện trên **server** (SSH vào trước).

### Trường hợp thường — chỉ thay đổi backend hoặc frontend

```bash
cd ~/EnglishEdu

# 1. Lấy code mới
git pull

# 2. Rebuild frontend (nếu có thay đổi trong sunshine-rebuild/)
cd sunshine-rebuild && npm run build && cd ..

# 3. Restart backend (không mất data, tự rebuild JAR)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend

# 4. Kiểm tra backend đã lên
docker compose -f docker-compose.prod.yml logs -f backend
# Chờ thấy: "Started ... in X seconds" → Ctrl+C
```

### Trường hợp thay đổi cả Nginx hoặc cấu hình Docker

```bash
cd ~/EnglishEdu
git pull
cd sunshine-rebuild && npm run build && cd ..

# Rebuild + restart tất cả services (không xóa data)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

# Theo dõi
docker compose -f docker-compose.prod.yml ps
# Tất cả STATUS phải "Up"
```

### Trường hợp chỉ restart nhanh (không thay đổi code)

```bash
cd ~/EnglishEdu

# Restart backend
docker compose -f docker-compose.prod.yml restart backend

# Hoặc restart tất cả
b```

### Kiểm tra sau khi restart

```bash
# Backend health
curl http://localhost:4000/actuator/health
# → {"status":"UP"}

# Xem log nếu có lỗi
docker compose -f docker-compose.prod.yml logs --tail=50 backend
```

---

## Xóa sạch và build lại từ đầu (Fresh DB)

Nếu muốn reset hoàn toàn (bao gồm Moodle DB mới):

```bash
cd ~/EnglishEdu

# Dừng tất cả + XÓA volumes (mất toàn bộ data Moodle, Postgres, MinIO)
docker compose -f docker-compose.prod.yml down -v --remove-orphans

# Bắt đầu lại từ BƯỚC 4 (nếu đã có code) hoặc BƯỚC 2 (nếu cần pull code mới)
```

> ⚠️ `down -v` xóa sạch volume → Moodle sẽ init DB mới từ đầu theo env vars.
> Sau đó vào BƯỚC 5 để verify `wwwroot` và cấu hình Web Services thủ công.

## Khi có domain — chuyển sang HTTPS

1. Trỏ DNS: `sunshineschool.edu.vn` + `lms.sunshineschool.edu.vn` → `221.132.21.13`
2. Lấy cert: `certbot certonly --standalone -d sunshineschool.edu.vn -d lms.sunshineschool.edu.vn`
3. Copy cert: `mkdir nginx/ssl && ln -s /etc/letsencrypt/live/.../fullchain.pem nginx/ssl/cert.pem && ln -s .../privkey.pem nginx/ssl/key.pem`
4. Thay `nginx/nginx.conf` bằng `nginx/nginx.conf.ssl-ready`
5. Bỏ comment dòng `- ./nginx/ssl:/etc/nginx/ssl:ro` trong `docker-compose.prod.yml`
6. Bỏ comment `- "443:443"` và đổi `http://` → `https://` trong APP_BASE_URL, CORS_ORIGINS
7. Cập nhật Moodle wwwroot: `UPDATE mdl_config SET value='https://lms.sunshineschool.edu.vn' WHERE name='wwwroot';`
8. `docker compose -f docker-compose.prod.yml --env-file .env.prod up -d`
