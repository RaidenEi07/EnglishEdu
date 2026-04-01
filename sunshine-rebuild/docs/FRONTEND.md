# Sunshine School Online — Tổng Quan Frontend

> **Dự án**: Rebuild giao diện website học tiếng Anh trực tuyến `sunshineschool.edu.vn`  
> **Ngày cập nhật**: 18/03/2026

---

## 1. Tổng Quan & Tech Stack

| Công nghệ | Phiên bản | Vai trò |
|---|---|---|
| **Vite** | 6.2+ | Dev server + build tool đa trang (MPA), HMR, bundling, minification |
| **Bootstrap** | 5.3.3 | UI framework — cài qua npm (không dùng CDN) |
| **Vanilla JavaScript** | ES Modules | Không dùng React/Vue/Angular |
| **Font Awesome** | 6.5.1 | Icon (CDN) |
| **Google Fonts** | Roboto | Typography (CDN) |
| **stylelint / eslint** | latest | Linting (dev only) |

**Biến môi trường** (`.env.example`):
```
VITE_API_BASE_URL=http://localhost:4000/api/v1
VITE_GA_ID=G-SBWXZQB198
```

Proxy trong Vite dev (`vite.config.js`): `/api` → `http://localhost:4000`

---

## 2. Cấu Trúc Thư Mục

```
sunshine-rebuild/
├── index.html                    ← Trang chủ (Home)
├── vite.config.js                ← Cấu hình Vite (entry points, proxy)
├── package.json
├── public/
│   ├── images/courses/           ← Ảnh tĩnh khóa học
│   └── locales/
│       ├── vi.json               ← Bản dịch tiếng Việt (mặc định)
│       └── en.json               ← Bản dịch tiếng Anh
├── src/
│   ├── shared/                   ← Dùng chung cho tất cả trang
│   │   ├── js/
│   │   │   ├── api.js            ← Fetch wrapper + auth headers + 401 redirect
│   │   │   ├── auth.js           ← login / logout / guestLogin / requestPasswordReset
│   │   │   ├── i18n.js           ← Đa ngôn ngữ: load JSON, applyTranslations, t()
│   │   │   ├── navbar.js         ← initNavbar(): menu user, auth guard, logout
│   │   │   ├── courseData.js     ← 24 khóa học tĩnh (fallback khi API lỗi)
│   │   │   └── dom.js            ← $(), $$(), on() helpers
│   │   └── styles/
│   │       └── shared.css        ← @import entry cho tất cả partials CSS
│   └── home/
│       ├── home.js               ← Entry trang chủ
│       ├── home.css
│       └── courses.js            ← Fetch + render + filter khóa học trang chủ
└── pages/
    ├── login/                    ← Đăng nhập
    ├── forgot-password/          ← Quên mật khẩu
    ├── my/
    │   ├── my-utils.js           ← fetchMyCourses(), setUserInitials()
    │   ├── dashboard/            ← Dashboard học viên
    │   └── courses/              ← Danh sách khóa học của tôi
    └── profile/                  ← Hồ sơ cá nhân
```

---

## 3. Trang & Routes

| Trang | File HTML | URL | Yêu cầu đăng nhập |
|---|---|---|---|
| **Trang chủ** | `index.html` | `/` | Không |
| **Đăng nhập** | `pages/login/index.html` | `/pages/login/` | Không |
| **Quên mật khẩu** | `pages/forgot-password/index.html` | `/pages/forgot-password/` | Không |
| **Dashboard** | `pages/my/dashboard/index.html` | `/pages/my/dashboard/` | **Có** |
| **Khóa học của tôi** | `pages/my/courses/index.html` | `/pages/my/courses/` | **Có** |
| **Hồ sơ** | `pages/profile/index.html` | `/pages/profile/` | **Có** |

> Trang chi tiết khóa học (`/pages/course.html?id={id}`) đã có link tham chiếu nhưng **chưa được xây dựng**.

---

## 4. Tất Cả API Endpoints được Gọi

Mọi request đều qua hàm `api()` trong `src/shared/js/api.js` (tự thêm `Authorization: Bearer <token>`).

| Method | Endpoint | Gọi từ | Mô tả |
|---|---|---|---|
| `POST` | `/auth/login` | `auth.js` | Đăng nhập username + password |
| `POST` | `/auth/guest` | `auth.js` | Đăng nhập ẩn danh (guest) |
| `POST` | `/auth/logout` | `auth.js` | Logout (fire-and-forget) |
| `POST` | `/auth/forgot-password` | `auth.js` | Gửi email đặt lại mật khẩu |
| `GET` | `/courses?size=50` | `src/home/courses.js` | Danh sách khóa học trang chủ |
| `GET` | `/courses/enrolled` | `my-utils.js` | Khóa học đang học của user |
| `GET` | `/courses/recent` | `dashboard.js` | 5 khóa học truy cập gần nhất |
| `GET` | `/timeline` | `dashboard.js` | Timeline/hoạt động học tập |
| `GET` | `/users/me` | `profile.js` | Tải thông tin cá nhân |
| `PUT` | `/users/me` | `profile.js` | Cập nhật firstName, lastName, email |
| `POST` | `/users/me/password` | `profile.js` | Đổi mật khẩu |
| `POST` | `/users/me/avatar` | `profile.js` | Upload ảnh đại diện (multipart) |

**Xử lý 401**: Tự động xóa `sso_token` khỏi `localStorage` và redirect sang trang đăng nhập.

---

## 5. Luồng Xác Thực (Authentication)

### Dữ liệu lưu trong `localStorage`

| Key | Giá trị |
|---|---|
| `sso_token` | JWT access token |
| `sso_user` | JSON: `{ id, username, firstName, lastName, email, avatarUrl, role, fullname }` |

### Luồng đăng nhập
1. Form gửi `username` + `password` → `POST /auth/login`
2. Server trả `{ token, user }` → lưu vào `localStorage`
3. Redirect sang trang trước đó (query `?redirect=`) hoặc trang chủ `/`

### Luồng Guest Login
`POST /auth/guest` → server tạo tài khoản ẩn danh (`guest_XXXXXXXX`) → trả token → lưu như đăng nhập thường.

### Luồng Logout
```js
fetch('/api/v1/auth/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
clearToken();  // xóa localStorage
window.location.href = '/';
```

### Route Guard (client-side)
Các trang bảo vệ kiểm tra `isLoggedIn()` ngay khi khởi động:
```js
if (!isLoggedIn()) {
  window.location.replace(`/pages/login/?redirect=${encodeURIComponent(window.location.pathname)}`);
  return;
}
```

---

## 6. Quản Lý State

**Không có thư viện reactive state**. State được quản lý qua:

| Phương pháp | Dùng cho |
|---|---|
| `localStorage` | Token JWT, thông tin user — tồn tại qua reload trang |
| Module-level variables | `let allCourses = []` — dữ liệu trong phiên của trang hiện tại |
| DOM as state | Toggle Bootstrap classes (`d-none`, `active`) để phản ánh trạng thái UI |

---

## 7. Đa Ngôn Ngữ (i18n)

**Module**: `src/shared/js/i18n.js`

- Ngôn ngữ: `vi` (mặc định) và `en`
- Lưu setting: `localStorage.sso_language`
- Files dịch: `/locales/vi.json` và `/locales/en.json` (~100 keys mỗi file)

**Cách dùng trong HTML**:
```html
<a data-i18n="nav.home">Trang chủ</a>
```

**Cách dùng trong JS**:
```js
import { t } from '../../src/shared/js/i18n.js';
const label = t('my_courses.in_progress');
```

**Nhóm keys dịch**: `nav.*`, `auth.*`, `dashboard.*`, `my_courses.*`, `profile.*`, `footer.*`, `courses.*`, `hero.*`, `marketing.*`

---

## 8. Kiến Trúc CSS

Dùng **partials-based CSS** với một entry point chia sẻ:

```
src/shared/styles/
├── shared.css              ← @import hub (import bởi mọi trang)
├── base/
│   ├── _variables.css      ← CSS custom properties (design tokens)
│   └── _reset.css          ← Universal reset
├── layout/
│   ├── _navbar.css         ← Navbar styles + scroll effect
│   └── _footer.css         ← Footer dark background
├── components/
│   ├── _hero.css           ← Hero slider
│   ├── _features.css       ← Marketing feature cards
│   ├── _cards.css          ← Course card hover lift
│   ├── _forms.css          ← Login/forgot form focus ring
│   └── _accordion.css      ← FAQ accordion
└── responsive/
    └── _breakpoints.css    ← Breakpoints: 575px và 767px
```

**Design Tokens** (`_variables.css`):
```css
:root {
  --sso-primary:   #0d6efd;   /* Bootstrap blue */
  --sso-accent:    #f7941d;   /* Orange */
  --sso-dark:      #212529;
  --navbar-height: 56px;
  --transition-fast: 0.2s ease;
  --radius-sm: 4px; --radius-md: 8px; --radius-lg: 12px;
}
```

Mỗi trang có file CSS riêng (`login.css`, `dashboard.css`, ...) cho override cụ thể.

---

## 9. Tính Năng Nổi Bật

### Trang chủ (`/`)
- Carousel hero slider với ảnh khóa học
- Danh sách khóa học với filter theo IELTS/Cambridge
- **Graceful degradation**: nếu API fail → hiển thị 24 khóa học tĩnh từ `courseData.js`
- Scroll effect: navbar thêm `box-shadow` khi `scrollY > 50`

### Dashboard (`/pages/my/dashboard/`)
- **Mini-calendar** tự build bằng pure JS: `<table>` với prev/next tháng, highlight ngày hôm nay
- **Progress stats**: tổng đang học, đang tiến hành (`0 < progress < 100`), đã hoàn thành (`progress === 100`)
- **Bootstrap progress bar** hiển thị % hoàn thành
- **Timeline filter**: lọc upcoming/overdue activity

### Khóa học của tôi (`/pages/my/courses/`)
- **3 chế độ xem**: Card (grid), List (ngang), Summary (compact text) — switch client-side
- **Bộ lọc**: All / In Progress / Future / Past / Starred / Hidden
- **Tìm kiếm** theo tên, **sắp xếp** theo title/lastAccess
- Tất cả xử lý client-side sau 1 lần gọi API

### Hồ sơ (`/pages/profile/`)
- **Optimistic avatar upload**: preview ảnh ngay lập tức bằng `FileReader.readAsDataURL()`, sau đó mới POST lên server
- **Đổi mật khẩu**: sau khi thành công → tự động `logout()` sau 3 giây

### Navbar
- Initials user: lấy chữ cái đầu của tên đầu và tên cuối (VD: "Nguyen Van" → "NV")
- Mobile: Bootstrap offcanvas side drawer (< md breakpoint)
- Links với `data-auth-required` → redirect login nếu chưa đăng nhập

---

## 10. Pattern Khởi Tạo Trang

Mọi file JS entry của trang đều theo cấu trúc:
```js
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import './page.css';
import 'bootstrap';
import { initI18n } from '../../src/shared/js/i18n.js';
import { initNavbar } from '../../src/shared/js/navbar.js';

function initApp() {
  initI18n();
  initNavbar();
  initPage();
}

document.readyState === 'loading'
  ? document.addEventListener('DOMContentLoaded', initApp)
  : initApp();
```

---

## 11. Dữ Liệu Tĩnh Fallback

24 khóa học hardcode trong `src/shared/js/courseData.js`:

| Category | Số lượng | External IDs |
|---|---|---|
| **IELTS** | 14 | 83, 84, 40, 68, 39, 27, 30, 33, 42, 41, 76, 81, 82, 74 |
| **Cambridge** | 10 | 25, 29, 38, 66, 70, 71, 67, 69, 72, 73 (A2→B2) |

---

*Xem thêm: [BACKEND.md](BACKEND.md) — Tài liệu Backend*
