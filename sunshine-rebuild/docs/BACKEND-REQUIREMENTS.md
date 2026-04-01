# Backend Requirements - Sunshine School Online (SSO)

## Mục lục
1. [Tổng Quan Kiến Trúc](#1-tổng-quan-kiến-trúc)
2. [Cơ Sở Dữ Liệu](#2-cơ-sở-dữ-liệu)
3. [API Endpoints](#3-api-endpoints)
4. [Authentication & Authorization](#4-authentication--authorization)
5. [Quản Lý Khóa Học (LMS Core)](#5-quản-lý-khóa-học-lms-core)
6. [Quản Lý Nội Dung (CMS)](#6-quản-lý-nội-dung-cms)
7. [Hệ Thống Quiz & Assessment](#7-hệ-thống-quiz--assessment)
8. [File Management](#8-file-management)
9. [Đa Ngôn Ngữ (i18n)](#9-đa-ngôn-ngữ-i18n)
10. [Thông Báo & Email](#10-thông-báo--email)
11. [Analytics & Reporting](#11-analytics--reporting)
12. [Yêu Cầu Phi Chức Năng](#12-yêu-cầu-phi-chức-năng)
13. [Pha Triển Khai Đề Xuất](#13-pha-triển-khai-đề-xuất)

---

## 1. Tổng Quan Kiến Trúc

### Stack đề xuất
```
Frontend:  Next.js 14+ (React) / hoặc Nuxt.js (Vue)
Backend:   Node.js + Express / hoặc NestJS
Database:  PostgreSQL (primary) + Redis (cache/session)
Storage:   S3-compatible (MinIO / AWS S3)
Auth:      JWT + Refresh Token
Queue:     Bull Queue (Redis-based) - xử lý email, report
Search:    Elasticsearch (tùy chọn - tìm kiếm khóa học)
```

### Kiến trúc hệ thống
```
┌─────────────┐     ┌──────────────────┐     ┌──────────────┐
│  Frontend   │────▶│  API Gateway     │────▶│  PostgreSQL  │
│  (Next.js)  │     │  (Express/Nest)  │     │              │
└─────────────┘     └──────┬───────────┘     └──────────────┘
                           │
                    ┌──────┼──────┐
                    ▼      ▼      ▼
              ┌────────┐┌─────┐┌──────┐
              │ Redis  ││ S3  ││Queue │
              │(Cache) ││     ││(Bull)│
              └────────┘└─────┘└──────┘
```

---

## 2. Cơ Sở Dữ Liệu

### 2.1 Entity Relationship Diagram (Chính)

```
users ─────────┬── enrollments ──── courses ──── course_categories
               │                      │
               │                      ├── lessons
               │                      ├── quizzes ──── quiz_questions ──── quiz_answers
               │                      └── assignments
               │
               ├── quiz_attempts ──── quiz_responses
               ├── progress_tracking
               └── user_sessions
```

### 2.2 Bảng Chi Tiết

#### `users` - Người dùng
```sql
CREATE TABLE users (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(100) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    avatar_url      VARCHAR(500),
    role            VARCHAR(20) NOT NULL DEFAULT 'student',  -- admin, teacher, student, guest
    status          VARCHAR(20) NOT NULL DEFAULT 'active',   -- active, inactive, suspended
    language        VARCHAR(5) DEFAULT 'vi',                 -- vi, en
    timezone        VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh',
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

#### `course_categories` - Danh mục khóa học
```sql
CREATE TABLE course_categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,        -- IELTS, CAMBRIDGE
    slug        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    sort_order  INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);
```

#### `courses` - Khóa học
```sql
CREATE TABLE courses (
    id              SERIAL PRIMARY KEY,
    category_id     INT REFERENCES course_categories(id),
    title           VARCHAR(500) NOT NULL,
    slug            VARCHAR(500) UNIQUE NOT NULL,
    description     TEXT,
    thumbnail_url   VARCHAR(500),
    status          VARCHAR(20) DEFAULT 'published',  -- draft, published, archived
    is_featured     BOOLEAN DEFAULT false,
    sort_order      INT DEFAULT 0,
    max_students    INT,
    created_by      INT REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

#### `lessons` - Bài học trong khóa học
```sql
CREATE TABLE lessons (
    id          SERIAL PRIMARY KEY,
    course_id   INT REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    content     TEXT,
    type        VARCHAR(30) NOT NULL,  -- video, text, document, interactive
    duration    INT,                   -- phút
    sort_order  INT DEFAULT 0,
    is_free     BOOLEAN DEFAULT false,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);
```

#### `enrollments` - Đăng ký khóa học
```sql
CREATE TABLE enrollments (
    id              SERIAL PRIMARY KEY,
    user_id         INT REFERENCES users(id) ON DELETE CASCADE,
    course_id       INT REFERENCES courses(id) ON DELETE CASCADE,
    status          VARCHAR(20) DEFAULT 'active',  -- active, completed, expired, suspended
    enrolled_at     TIMESTAMP DEFAULT NOW(),
    completed_at    TIMESTAMP,
    expiry_date     TIMESTAMP,
    UNIQUE(user_id, course_id)
);
```

#### `quizzes` - Bài kiểm tra
```sql
CREATE TABLE quizzes (
    id              SERIAL PRIMARY KEY,
    course_id       INT REFERENCES courses(id) ON DELETE CASCADE,
    lesson_id       INT REFERENCES lessons(id) ON DELETE SET NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    type            VARCHAR(30) NOT NULL,     -- practice, mocktest, final_test
    time_limit      INT,                      -- phút, NULL = không giới hạn
    max_attempts    INT DEFAULT 1,
    passing_score   DECIMAL(5,2) DEFAULT 50,  -- %
    shuffle_questions BOOLEAN DEFAULT false,
    show_answers    BOOLEAN DEFAULT false,     -- hiển thị đáp án sau khi nộp
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

#### `quiz_questions` - Câu hỏi
```sql
CREATE TABLE quiz_questions (
    id          SERIAL PRIMARY KEY,
    quiz_id     INT REFERENCES quizzes(id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL,   -- multiple_choice, true_false, fill_blank,
                                        -- matching, essay, listening, reading_passage
    content     TEXT NOT NULL,
    media_url   VARCHAR(500),           -- audio/image cho câu hỏi listening
    points      DECIMAL(5,2) DEFAULT 1,
    sort_order  INT DEFAULT 0,
    explanation TEXT,                    -- giải thích đáp án
    created_at  TIMESTAMP DEFAULT NOW()
);
```

#### `quiz_answers` - Đáp án
```sql
CREATE TABLE quiz_answers (
    id          SERIAL PRIMARY KEY,
    question_id INT REFERENCES quiz_questions(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    is_correct  BOOLEAN DEFAULT false,
    sort_order  INT DEFAULT 0
);
```

#### `quiz_attempts` - Lượt làm bài
```sql
CREATE TABLE quiz_attempts (
    id          SERIAL PRIMARY KEY,
    quiz_id     INT REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id     INT REFERENCES users(id) ON DELETE CASCADE,
    score       DECIMAL(5,2),
    max_score   DECIMAL(5,2),
    percentage  DECIMAL(5,2),
    status      VARCHAR(20) DEFAULT 'in_progress',  -- in_progress, submitted, graded
    started_at  TIMESTAMP DEFAULT NOW(),
    submitted_at TIMESTAMP,
    time_spent  INT                                  -- seconds
);
```

#### `quiz_responses` - Câu trả lời của học sinh
```sql
CREATE TABLE quiz_responses (
    id          SERIAL PRIMARY KEY,
    attempt_id  INT REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id INT REFERENCES quiz_questions(id),
    answer_id   INT REFERENCES quiz_answers(id),    -- cho multiple choice
    text_answer TEXT,                                -- cho essay/fill blank
    is_correct  BOOLEAN,
    points      DECIMAL(5,2) DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);
```

#### `progress_tracking` - Theo dõi tiến độ
```sql
CREATE TABLE progress_tracking (
    id          SERIAL PRIMARY KEY,
    user_id     INT REFERENCES users(id) ON DELETE CASCADE,
    course_id   INT REFERENCES courses(id) ON DELETE CASCADE,
    lesson_id   INT REFERENCES lessons(id) ON DELETE CASCADE,
    status      VARCHAR(20) DEFAULT 'not_started',  -- not_started, in_progress, completed
    completed_at TIMESTAMP,
    UNIQUE(user_id, lesson_id)
);
```

#### `site_settings` - Cấu hình trang
```sql
CREATE TABLE site_settings (
    id      SERIAL PRIMARY KEY,
    key     VARCHAR(100) UNIQUE NOT NULL,
    value   TEXT,
    type    VARCHAR(20) DEFAULT 'text'  -- text, json, html, image_url
);
-- Dữ liệu: logo, favicon, slider_images, marketing_features, faq_items,
-- contact_phone, contact_website, addresses, working_hours, footer_text
```

#### `translations` - Bản dịch nội dung
```sql
CREATE TABLE translations (
    id          SERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,   -- course, lesson, quiz, site_setting
    entity_id   INT NOT NULL,
    field       VARCHAR(100) NOT NULL,  -- title, description, content
    language    VARCHAR(5) NOT NULL,    -- vi, en
    value       TEXT NOT NULL,
    UNIQUE(entity_type, entity_id, field, language)
);
```

---

## 3. API Endpoints

### 3.1 Authentication
```
POST   /api/auth/login              Đăng nhập (username/email + password)
POST   /api/auth/logout             Đăng xuất
POST   /api/auth/refresh            Refresh access token
POST   /api/auth/forgot-password    Gửi email reset password
POST   /api/auth/reset-password     Đặt lại mật khẩu
POST   /api/auth/guest-login        Đăng nhập khách (giới hạn quyền)
GET    /api/auth/me                 Lấy thông tin user hiện tại
```

### 3.2 Users (Admin)
```
GET    /api/users                   Danh sách users (phân trang, tìm kiếm)
GET    /api/users/:id               Chi tiết user
POST   /api/users                   Tạo user mới
PUT    /api/users/:id               Cập nhật user
DELETE /api/users/:id               Xóa/suspend user
PUT    /api/users/:id/role          Thay đổi role
GET    /api/users/:id/courses       Danh sách khóa học của user
GET    /api/users/:id/progress      Tiến độ học tập
```

### 3.3 Courses
```
GET    /api/courses                 Danh sách khóa học (public, filter category/search)
GET    /api/courses/:slug           Chi tiết khóa học
POST   /api/courses                 Tạo khóa học (admin/teacher)
PUT    /api/courses/:id             Cập nhật khóa học
DELETE /api/courses/:id             Xóa khóa học
POST   /api/courses/:id/enroll      Đăng ký khóa học
GET    /api/courses/:id/students    Danh sách học viên
GET    /api/courses/:id/lessons     Danh sách bài học
GET    /api/courses/:id/progress    Tiến độ tổng thể
```

### 3.4 Categories
```
GET    /api/categories              Danh sách categories
POST   /api/categories              Tạo category (admin)
PUT    /api/categories/:id          Cập nhật category
DELETE /api/categories/:id          Xóa category
```

### 3.5 Lessons
```
GET    /api/lessons/:id             Chi tiết bài học
POST   /api/courses/:id/lessons     Tạo bài học
PUT    /api/lessons/:id             Cập nhật bài học
DELETE /api/lessons/:id             Xóa bài học
POST   /api/lessons/:id/complete    Đánh dấu hoàn thành
```

### 3.6 Quizzes & Tests
```
GET    /api/quizzes/:id             Chi tiết quiz
POST   /api/courses/:id/quizzes     Tạo quiz (admin/teacher)
PUT    /api/quizzes/:id             Cập nhật quiz
DELETE /api/quizzes/:id             Xóa quiz
POST   /api/quizzes/:id/start       Bắt đầu làm bài
POST   /api/quizzes/:id/submit      Nộp bài
GET    /api/quizzes/:id/attempts     Lịch sử làm bài
GET    /api/attempts/:id/review      Xem lại bài làm
```

### 3.7 Quiz Questions (Admin/Teacher)
```
GET    /api/quizzes/:id/questions    Danh sách câu hỏi
POST   /api/quizzes/:id/questions    Thêm câu hỏi
PUT    /api/questions/:id            Cập nhật câu hỏi
DELETE /api/questions/:id            Xóa câu hỏi
POST   /api/quizzes/:id/questions/import  Import câu hỏi từ file
```

### 3.8 Site Settings (Admin/CMS)
```
GET    /api/settings                 Lấy public settings (logo, contact, etc.)
PUT    /api/settings                 Cập nhật settings (admin)
GET    /api/settings/slider          Lấy slider images
PUT    /api/settings/slider          Cập nhật slider
GET    /api/settings/marketing       Lấy marketing features
PUT    /api/settings/marketing       Cập nhật marketing features
GET    /api/settings/faq             Lấy FAQ items
PUT    /api/settings/faq             Cập nhật FAQ items
```

### 3.9 File Upload
```
POST   /api/upload/image             Upload ảnh (avatar, thumbnail, marketing icon)
POST   /api/upload/document          Upload tài liệu (PDF, DOCX)
POST   /api/upload/audio             Upload audio (listening)
DELETE /api/upload/:id               Xóa file
```

### 3.10 Reports & Analytics (Admin)
```
GET    /api/reports/overview         Dashboard tổng quan
GET    /api/reports/courses/:id      Báo cáo theo khóa học
GET    /api/reports/users/:id        Báo cáo theo học viên
GET    /api/reports/quizzes/:id      Thống kê quiz
GET    /api/reports/enrollment       Thống kê đăng ký
GET    /api/reports/export           Xuất báo cáo (CSV/Excel)
```

---

## 4. Authentication & Authorization

### 4.1 Flow Đăng Nhập
```
1. User gửi POST /api/auth/login { username, password }
2. Server xác thực credentials
3. Trả về { accessToken (15min), refreshToken (7d) }
4. Frontend lưu accessToken trong memory, refreshToken trong HttpOnly cookie
5. Mỗi request gửi accessToken trong Authorization header
6. Khi accessToken hết hạn -> gọi /api/auth/refresh
```

### 4.2 Roles & Permissions
| Quyền | Admin | Teacher | Student | Guest |
|-------|-------|---------|---------|-------|
| Quản lý users | ✅ | ❌ | ❌ | ❌ |
| CRUD courses | ✅ | ✅ (của mình) | ❌ | ❌ |
| CRUD quizzes | ✅ | ✅ (của mình) | ❌ | ❌ |
| Xem khóa học | ✅ | ✅ | ✅ (enrolled) | ✅ (free) |
| Làm quiz | ✅ | ✅ | ✅ (enrolled) | ❌ |
| Xem reports | ✅ | ✅ (của mình) | ❌ | ❌ |
| CMS settings | ✅ | ❌ | ❌ | ❌ |
| Enroll course | ✅ | ✅ | ✅ | ❌ |

### 4.3 Quên Mật Khẩu
```
1. User gửi POST /api/auth/forgot-password { usernameOrEmail }
2. Server tìm user, tạo reset token (expire 1h)
3. Gửi email chứa link reset
4. User click link -> frontend hiển thị form đặt mật khẩu mới
5. POST /api/auth/reset-password { token, newPassword }
```

---

## 5. Quản Lý Khóa Học (LMS Core)

### 5.1 Course Structure
```
Course
├── Thông tin chung (title, description, thumbnail, category)
├── Lessons (bài học - sắp xếp theo thứ tự)
│   ├── Lesson 1: Text content
│   ├── Lesson 2: Video
│   ├── Lesson 3: Document (PDF)
│   └── ...
├── Quizzes (bài kiểm tra)
│   ├── Practice Quiz
│   ├── Mock Test
│   └── Final Test
└── Students (danh sách học viên enrolled)
```

### 5.2 Enrollment Logic
- Admin/Teacher có thể enroll học viên vào khóa học
- Học viên tự đăng ký (nếu course cho phép)
- Hỗ trợ enrollment có thời hạn (expiry_date)
- Học viên hoàn thành tất cả lessons + đạt final test = completed

### 5.3 Progress Tracking
- Theo dõi từng bài học: not_started → in_progress → completed
- Tính % hoàn thành: `completedLessons / totalLessons * 100`
- Dashboard hiển thị tiến độ cho cả student và teacher

---

## 6. Quản Lý Nội Dung (CMS)

### 6.1 Dữ liệu cần quản lý từ Admin Panel

| Module | Dữ liệu | Kiểu |
|--------|----------|------|
| **Branding** | Logo, Favicon | Image URL |
| **Slider** | Banner images (1-5 slides) | Image URL + caption |
| **Marketing** | 4 feature cards (icon, title, content) | JSON |
| **FAQ** | Danh sách Q&A | JSON array |
| **Contact** | Phone, website, email | Text |
| **Addresses** | 3 cơ sở (tên + địa chỉ) | JSON array |
| **Working Hours** | Lịch làm việc | JSON |
| **Footer** | Copyright text | Text |
| **Social** | Facebook, YouTube, etc. | URL |

### 6.2 Seed Data (Dữ liệu ban đầu)
```json
{
  "categories": [
    { "name": "IELTS", "slug": "ielts" },
    { "name": "CAMBRIDGE", "slug": "cambridge" }
  ],
  "contact": {
    "phone": ["0979 289 466", "0985 289 466"],
    "website": "pbs.sunshineschool.io.vn"
  },
  "addresses": [
    { "label": "Trụ sở chính", "address": "K7/210 đường Hoàng Quốc Việt, Q.Cầu Giấy, TP.Hà Nội" },
    { "label": "Cơ sở 1", "address": "33 Nguyễn Thị Minh Khai, P.Xương Giang, TP.Bắc Giang" },
    { "label": "Cơ sở 2", "address": "Lô 09 đường Pháp Loa, TDP 4, TT.Nham Biền, H.Yên Dũng, T.Bắc Giang" }
  ],
  "workingHours": [
    { "days": "Thứ 3 - Thứ 6", "time": "8:00 - 21:30" },
    { "days": "Thứ 7, Chủ nhật", "time": "7:30 - 19:00" },
    { "days": "Thứ 2", "time": "Nghỉ" }
  ]
}
```

---

## 7. Hệ Thống Quiz & Assessment

### 7.1 Loại câu hỏi hỗ trợ
| Loại | Mô tả | Auto-grade |
|------|--------|------------|
| `multiple_choice` | Chọn 1 đáp án đúng | ✅ |
| `multi_select` | Chọn nhiều đáp án | ✅ |
| `true_false` | Đúng/Sai | ✅ |
| `fill_blank` | Điền vào chỗ trống | ✅ (exact match) |
| `matching` | Nối cặp | ✅ |
| `listening` | Nghe + trả lời (kèm audio) | ✅ |
| `reading_passage` | Đọc đoạn văn + trả lời | ✅ |
| `essay` | Viết tự luận | ❌ (teacher chấm) |

### 7.2 IELTS Mock Test Format
```
Mock Test
├── Listening (4 sections, ~40 câu, 30 phút)
│   ├── Section 1: Fill-in-the-blank
│   ├── Section 2: Multiple choice
│   ├── Section 3: Matching
│   └── Section 4: Fill-in-the-blank
├── Reading (3 passages, ~40 câu, 60 phút)
│   ├── Passage 1: True/False/Not Given + Fill blank
│   ├── Passage 2: Matching headings + Multiple choice
│   └── Passage 3: Multiple choice + Fill blank
├── Writing (2 tasks, 60 phút) - Teacher graded
│   ├── Task 1: Describe chart/graph
│   └── Task 2: Essay
└── Speaking (3 parts) - Teacher graded / ELSA integrated
    ├── Part 1: Introduction
    ├── Part 2: Cue card
    └── Part 3: Discussion
```

### 7.3 Auto-grading Flow
```
1. Student submit bài
2. Server so sánh câu trả lời với đáp án
3. Tính điểm: sum(correct_points) / sum(total_points) * 100
4. Lưu kết quả vào quiz_attempts
5. Nếu có essay/speaking -> status = 'pending_review'
6. Teacher review & chấm điểm essay
7. Tổng hợp điểm cuối cùng
```

---

## 8. File Management

### 8.1 Yêu cầu
- Upload ảnh: max 5MB, formats: JPG, PNG, WebP
- Upload audio: max 50MB, formats: MP3, WAV, OGG
- Upload document: max 20MB, formats: PDF, DOCX
- Hỗ trợ image resize/optimize tự động
- CDN delivery cho static files

### 8.2 Storage Structure
```
uploads/
├── avatars/           {userId}/{filename}
├── courses/           {courseId}/thumbnail/{filename}
├── lessons/           {lessonId}/{filename}
├── quizzes/           {quizId}/audio/{filename}
├── site/
│   ├── logo/          {filename}
│   ├── favicon/       {filename}
│   ├── slider/        {filename}
│   └── marketing/     {filename}
└── temp/              (tạm, tự dọn sau 24h)
```

---

## 9. Đa Ngôn Ngữ (i18n)

### 9.1 Chiến lược
- **UI labels**: File JSON tĩnh trên frontend (`vi.json`, `en.json`)
- **Dynamic content** (course title, description, FAQ, marketing): Bảng `translations` trong DB
- **URL routing**: `/vi/courses/...`, `/en/courses/...` hoặc query param `?lang=en`

### 9.2 Frontend Translation Keys
```json
// vi.json
{
  "nav.home": "Trang chủ",
  "nav.login": "Đăng nhập",
  "nav.more": "Xem thêm",
  "courses.title": "Các khoá học hiện tại",
  "courses.skip": "Bỏ qua các khoá học hiện tại",
  "auth.login": "Đăng nhập",
  "auth.username": "Tên đăng nhập",
  "auth.password": "Mật khẩu",
  "auth.forgot": "Quên mật khẩu?",
  "auth.guest": "Đăng nhập với tư cách khách",
  "auth.not_logged_in": "Bạn chưa đăng nhập.",
  "footer.contact": "Contact us",
  "footer.follow": "Follow us",
  "footer.address": "Địa chỉ",
  "footer.working_hours": "Thời gian làm việc",
  "footer.get_app": "Tải ứng dụng"
}
```

---

## 10. Thông Báo & Email

### 10.1 Email Templates
| Trigger | Template | Biến |
|---------|----------|------|
| Đăng ký tài khoản | `welcome` | name, loginUrl |
| Reset password | `reset-password` | name, resetUrl, expiry |
| Enroll khóa học | `enrollment` | name, courseName, courseUrl |
| Quiz submitted | `quiz-result` | name, quizName, score |
| Course completed | `course-complete` | name, courseName, certificateUrl |

### 10.2 Queue Processing
- Sử dụng Bull Queue + Redis
- Email gửi async, retry 3 lần
- Rate limit: max 100 emails/phút

---

## 11. Analytics & Reporting

### 11.1 Dashboard Admin
- Tổng số users (active, inactive)
- Tổng số courses, enrollments
- Enrollment trend (chart theo tháng)
- Top courses (theo enrollment count)
- Quiz performance overview
- User activity log

### 11.2 Teacher Dashboard
- Danh sách courses của mình
- Số học viên mỗi course
- Tỷ lệ hoàn thành trung bình
- Bài quiz chờ chấm (essay)
- Điểm trung bình mỗi quiz

### 11.3 Student Dashboard
- Courses đã đăng ký
- Tiến độ mỗi course (progress bar)
- Lịch sử quiz (điểm, thời gian)
- Upcoming deadlines

---

## 12. Yêu Cầu Phi Chức Năng

### 12.1 Performance
- API response time: < 200ms (p95)
- Hỗ trợ 500+ concurrent users
- Database query < 50ms
- Image lazy loading, CDN caching

### 12.2 Security
- Password hashing: bcrypt (cost 12)
- JWT access token: 15 phút, refresh: 7 ngày
- Rate limiting: 100 requests/phút per IP
- CORS whitelist
- Input validation & sanitization (XSS, SQL injection prevention)
- HTTPS only
- Helmet.js headers
- File upload validation (type, size, malware scan)

### 12.3 Scalability
- Stateless API (scale horizontally)
- Redis session store
- S3 file storage (không lưu local)
- Database connection pooling
- Pagination cho mọi list API (default 20, max 100)

### 12.4 Monitoring
- Health check endpoint: GET /api/health
- Error logging: Sentry / Winston
- Request logging: Morgan
- Uptime monitoring

---

## 13. Pha Triển Khai Đề Xuất

### Phase 1: Foundation (MVP)
- [ ] Setup project (Node.js + Express + PostgreSQL)
- [ ] Database migrations & seed data
- [ ] Authentication (login, logout, refresh, forgot password)
- [ ] User CRUD (admin)
- [ ] Course Categories CRUD
- [ ] Courses CRUD + public listing
- [ ] File upload (images)
- [ ] Site Settings API (logo, contact, slider, marketing)
- [ ] i18n (vi/en)
- **Kết quả**: Frontend có thể hiển thị homepage, login, course listing

### Phase 2: LMS Core
- [ ] Enrollment system
- [ ] Lessons CRUD
- [ ] Progress tracking
- [ ] Student dashboard
- [ ] Teacher dashboard
- [ ] Basic quiz (multiple choice, true/false)
- [ ] Auto-grading

### Phase 3: Advanced Assessment
- [ ] Full quiz types (listening, reading, matching, fill blank, essay)
- [ ] IELTS Mock Test format
- [ ] Teacher quiz review (essay grading)
- [ ] Quiz import from file
- [ ] Audio upload & streaming (listening tests)

### Phase 4: Polish & Scale
- [ ] Reports & Analytics dashboard
- [ ] Email notifications (Bull Queue)
- [ ] Search (Elasticsearch)
- [ ] Mobile app API optimization
- [ ] Performance tuning & caching
- [ ] Backup & disaster recovery

---

## Ghi Chú Kỹ Thuật

### Dữ liệu cần migrate từ site gốc
1. **24 khóa học** → Import vào bảng `courses`
2. **2 categories** (IELTS, CAMBRIDGE) → Import vào `course_categories`
3. **Slider image** → Upload lại vào S3/storage
4. **4 marketing icons** → Upload lại
5. **Logo + Favicon** → Upload lại
6. **FAQ** → Cần tạo nội dung thực (hiện tại là placeholder "loremissum")
7. **User data** → Nếu có access vào Moodle DB gốc, export và import

### Khác biệt so với Moodle gốc
| Tính năng | Moodle | Hệ thống mới |
|-----------|--------|---------------|
| Architecture | Monolithic PHP | API-first (Node.js) |
| Frontend | Server-rendered (YUI) | SPA (React/Next.js) |
| Theme | Moove (Moodle theme) | Custom design |
| Database | MySQL | PostgreSQL |
| File storage | Local disk | S3-compatible |
| Auth | Session-based | JWT + Refresh Token |
| API | None (form-based) | RESTful JSON API |
| Quiz engine | Built-in Moodle | Custom-built |
