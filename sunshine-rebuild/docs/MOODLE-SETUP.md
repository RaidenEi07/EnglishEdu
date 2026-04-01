# Hướng dẫn cài đặt & tích hợp Moodle

## 1. Yêu cầu hệ thống

| Phần mềm | Phiên bản |
|-----------|-----------|
| PHP | ≥ 8.2 |
| PostgreSQL | ≥ 14 |
| Apache / Nginx / IIS | Bất kỳ |
| Moodle | 5.1.3+ (đã có trong `moodle/`) |

---

## 2. Tạo database cho Moodle

Moodle sử dụng database **riêng** (không dùng chung với EnglishEdu).

```sql
-- Chạy trong pgAdmin hoặc psql:
CREATE DATABASE moodle
  WITH ENCODING = 'UTF8'
       LC_COLLATE = 'C'
       LC_CTYPE = 'C'
       TEMPLATE = template0;
```

---

## 3. Tạo thư mục `moodledata`

Moodle cần thư mục **ngoài** web root để lưu file upload.

```powershell
mkdir C:\moodledata
```

> Đảm bảo quyền ghi cho PHP process.

---

## 4. Cấu hình web server (Apache)

### Tùy chọn A: XAMPP / WAMP

1. Copy thư mục `moodle/` vào `C:\xampp\htdocs\moodle` (hoặc symlink).
2. Mở `httpd.conf`, thêm virtual host **port 8088**:

```apache
Listen 8088
<VirtualHost *:8088>
    DocumentRoot "C:/My Web Sites/EnglishEdu/moodle"
    ServerName localhost
    <Directory "C:/My Web Sites/EnglishEdu/moodle">
        AllowOverride All
        Require all granted
    </Directory>
</VirtualHost>
```

3. Restart Apache.

### Tùy chọn B: PHP Built-in Server (nhanh, chỉ để dev)

```powershell
cd "C:\My Web Sites\EnglishEdu\moodle"
php -S localhost:8088
```

---

## 5. Chạy Moodle Installer

1. Mở trình duyệt: **http://localhost:8088**
2. Moodle installer sẽ tự động chạy (vì `config.php` đã được tạo sẵn).
3. Làm theo wizard:
   - Database: PostgreSQL, host `localhost`, db `moodle`, user `postgres`, pass `GilArchon`
   - Data directory: `C:\moodledata`
4. Tạo tài khoản **admin** (nhớ username + password).
5. Hoàn tất cài đặt.

---

## 6. Bật Web Services trên Moodle

Sau khi cài xong, đăng nhập admin vào Moodle:

### 6.1 Bật Web Services
1. Vào **Site administration → General → Advanced features**
2. Tick ✅ **Enable web services** → Save

### 6.2 Bật REST protocol
1. Vào **Site administration → Server → Web services → Manage protocols**
2. Bật (enable) **REST protocol**

### 6.3 Tạo External Service
1. Vào **Site administration → Server → Web services → External services**
2. Click **"Add"**
3. Tên: `EnglishEdu Integration`
4. Tick ✅ **Enabled**, ✅ **Authorised users only**
5. Save
6. Click **"Functions"** trên dòng vừa tạo
7. Thêm các functions sau:
   - `core_webservice_get_site_info`
   - `core_user_create_users`
   - `core_user_get_users`
   - `core_course_create_courses`
   - `core_course_get_contents`
   - `enrol_manual_enrol_users`
   - `core_enrol_get_enrolled_users`
   - `core_enrol_get_users_courses`
   - `gradereport_user_get_grades_table`
   - `auth_email_get_signup_settings` (optional)

### 6.4 Tạo Token (MOODLE_TOKEN)
1. Vào **Site administration → Server → Web services → Manage tokens**
2. Click **"Create token"**
3. Chọn user: **Admin** (tài khoản admin Moodle)
4. Chọn service: **EnglishEdu Integration**
5. Save
6. **Copy token** → Đây chính là giá trị `MOODLE_TOKEN`

---

## 7. Cấu hình EnglishEdu Backend

### 7.1 Qua biến môi trường (khuyến nghị)

```powershell
$env:MOODLE_URL = "http://localhost:8088"
$env:MOODLE_TOKEN = "abc123def456..."  # Token vừa copy ở bước 6.4
$env:MOODLE_SSO_SECRET = "your-random-secret-string-at-least-32-chars"
```

Hoặc tạo file `.env` (nếu dùng dotenv):

```env
MOODLE_URL=http://localhost:8088
MOODLE_TOKEN=abc123def456...
MOODLE_SSO_SECRET=your-random-secret-string-at-least-32-chars
```

### 7.2 Trực tiếp trong `application.properties` (chỉ dev)

File đã được cấu hình với default values:

```properties
moodle.url=${MOODLE_URL:http://localhost:8088}
moodle.token=${MOODLE_TOKEN:}
moodle.sso-secret=${MOODLE_SSO_SECRET:change-me-strong-random-secret}
```

### 7.3 Giải thích 3 giá trị

| Biến | Mô tả | Cách lấy |
|------|--------|----------|
| `MOODLE_URL` | URL gốc của Moodle | Thường là `http://localhost:8088` |
| `MOODLE_TOKEN` | Web service token của Moodle | Tạo ở bước 6.4 |
| `MOODLE_SSO_SECRET` | Secret key để ký SSO tokens | Tự tạo chuỗi ngẫu nhiên (≥32 ký tự) |

---

## 8. Kiểm tra kết nối

1. Khởi động Moodle server (port 8088)
2. Khởi động EnglishEdu backend (port 4000)
3. Khởi động frontend (port 5173)
4. Đăng nhập admin vào EnglishEdu
5. Vào trang **Quản trị** → Click **"Kiểm tra kết nối"** trong phần Moodle Integration
6. Nếu thấy ✓ → Thành công!

### Đồng bộ dữ liệu lần đầu

1. Click **"Đồng bộ người dùng → Moodle"** để tạo tài khoản Moodle cho tất cả user
2. Click **"Đồng bộ khóa học → Moodle"** để tạo khóa học Moodle cho tất cả course

Sau này, việc đồng bộ sẽ tự động khi:
- Admin tạo user mới → tự động tạo tài khoản Moodle
- Admin tạo khóa học mới → tự động tạo trên Moodle  
- Học sinh đăng ký khóa học (được duyệt) → tự động enrol vào Moodle

---

## 9. Sử dụng

### Cho học sinh
- Trang chi tiết khóa học: Nút **"Vào Moodle làm bài"** (xuất hiện khi đã enrolled + course có Moodle ID)
- Trang học: Nút **"Vào Moodle làm bài"** ở sidebar

### Cho admin
- Dashboard admin: Kiểm tra kết nối, đồng bộ hàng loạt
- Mọi thao tác tạo user/course/enrollment đều tự động sync

---

## 10. Kiến trúc tích hợp

```
┌──────────────┐     REST API      ┌──────────────┐
│  EnglishEdu  │ ──────────────────→│    Moodle    │
│   Backend    │   (Web Services)  │     LMS      │
│  (port 4000) │ ←─────────────────│  (port 8088) │
└──────┬───────┘                   └──────────────┘
       │                                    ↑
       │                                    │
┌──────┴───────┐          Window.open()     │
│  EnglishEdu  │ ──────────────────────────→│
│   Frontend   │   (Student opens Moodle)
│  (port 5173) │
└──────────────┘
```

### Files đã thêm/sửa:

**Backend (mới):**
- `com.sso.moodle.MoodleProperties` — Config từ `application.properties`
- `com.sso.moodle.MoodleClient` — REST client cho Moodle Web Services
- `com.sso.moodle.MoodleApiException` — Exception class
- `com.sso.moodle.MoodleSyncService` — Logic đồng bộ user/course/enrollment
- `com.sso.controller.MoodleController` — API endpoints
- `db/migration/V22__add_moodle_fields.sql` — Thêm cột moodle_id, moodle_course_id

**Backend (sửa):**
- `User.java` — Thêm `moodleId`
- `Course.java` — Thêm `moodleCourseId`
- `UserResponse.java` / `CourseResponse.java` — Thêm Moodle fields
- `CourseService.java` — Auto-sync on course creation
- `EnrollmentService.java` — Auto-sync on enroll/approve
- `UserService.java` — Auto-sync on user creation
- `application.properties` — Moodle config properties

**Frontend (sửa):**
- `types.ts` — Thêm `MoodleLaunchResponse`, cập nhật types
- `course/index.html` + `course.ts` — Nút "Vào Moodle làm bài"
- `learn/index.html` + `learn.ts` — Nút Moodle ở sidebar
- `admin/index.html` + `admin.ts` — Moodle sync controls
