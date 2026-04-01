# Hướng dẫn Deploy EnglishEdu lên Server

> **Server:** Ubuntu 22.04 · IP: `14.225.192.133` · Chưa có domain · HTTP only

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
ssh root@14.225.192.133

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
DOMAIN=14.225.192.133
LMS_DOMAIN=14.225.192.133
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

## BƯỚC 5 — Import Moodle DB (bắt buộc — chỉ lần đầu)

> Bitnami MariaDB image KHÔNG tự import file từ `docker-entrypoint-initdb.d`.
> Phải import thủ công.

```bash
cd ~/EnglishEdu

# 5.1 — Copy SQL vào container
docker compose -f docker-compose.prod.yml \
  cp moddle-lms/moodle-db-export.sql mariadb:/tmp/moodle.sql

docker compose -f docker-compose.prod.yml \
  cp moddle-lms/z-cleanup-users.sql mariadb:/tmp/cleanup.sql

# 5.2 — Import DB chính (~3-5 phút)
docker compose -f docker-compose.prod.yml exec mariadb bash -c \
  "/opt/bitnami/mariadb/bin/mariadb -u bn_moodle -pbn_pass bitnami_moodle < /tmp/moodle.sql"

# 5.3 — Chạy cleanup (xóa user test)
docker compose -f docker-compose.prod.yml exec mariadb bash -c \
  "/opt/bitnami/mariadb/bin/mariadb -u bn_moodle -pbn_pass bitnami_moodle < /tmp/cleanup.sql"

# 5.4 — Cập nhật wwwroot
docker compose -f docker-compose.prod.yml exec mariadb \
  /opt/bitnami/mariadb/bin/mariadb -u bn_moodle -pbn_pass bitnami_moodle \
  -e "UPDATE mdl_config SET value='http://14.225.192.133:8080' WHERE name='wwwroot';"

# 5.5 — Xóa cache Moodle + restart
docker compose -f docker-compose.prod.yml exec moodle bash -c \
  "rm -rf /bitnami/moodledata/cache/* /bitnami/moodledata/localcache/* /bitnami/moodledata/temp/*"

docker compose -f docker-compose.prod.yml restart moodle
```

**Kiểm tra:** Truy cập `http://14.225.192.133:8080` → phải thấy tên site đúng + 24 khóa học.

---

## BƯỚC 6 — Lấy Moodle Token

1. Mở `http://14.225.192.133:8080`
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
curl -I http://14.225.192.133
# → HTTP/1.1 200 OK
```

Truy cập trình duyệt:
- **Website:** http://14.225.192.133
- **Moodle:** http://14.225.192.133:8080
- **MinIO Console:** http://14.225.192.133:9001

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

## Xóa sạch và build lại từ đầu

Nếu gặp lỗi và cần reset hoàn toàn:

```bash
cd ~/EnglishEdu

# Dừng tất cả + XÓA volumes (mất data)
docker compose -f docker-compose.prod.yml down -v --remove-orphans --rmi all

# Xóa folder cũ
cd ~
rm -rf ~/EnglishEdu

# Bắt đầu lại từ BƯỚC 2
```

---

## Cập nhật code sau này

```bash
cd ~/EnglishEdu
git pull

# Rebuild frontend
cd sunshine-rebuild && npm run build && cd ..

# Restart backend (không mất data)
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend

# Nếu cần restart tất cả
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

---

## Khi có domain — chuyển sang HTTPS

1. Trỏ DNS: `sunshineschool.edu.vn` + `lms.sunshineschool.edu.vn` → `14.225.192.133`
2. Lấy cert: `certbot certonly --standalone -d sunshineschool.edu.vn -d lms.sunshineschool.edu.vn`
3. Copy cert: `mkdir nginx/ssl && ln -s /etc/letsencrypt/live/.../fullchain.pem nginx/ssl/cert.pem && ln -s .../privkey.pem nginx/ssl/key.pem`
4. Thay `nginx/nginx.conf` bằng `nginx/nginx.conf.ssl-ready`
5. Bỏ comment dòng `- ./nginx/ssl:/etc/nginx/ssl:ro` trong `docker-compose.prod.yml`
6. Bỏ comment `- "443:443"` và đổi `http://` → `https://` trong APP_BASE_URL, CORS_ORIGINS
7. Cập nhật Moodle wwwroot: `UPDATE mdl_config SET value='https://lms.sunshineschool.edu.vn' WHERE name='wwwroot';`
8. `docker compose -f docker-compose.prod.yml --env-file .env.prod up -d`
