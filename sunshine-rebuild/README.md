# SSO - Sunshine School Online

Dự án rebuild website Sunshine School Online (sunshineschool.edu.vn) — Vite + Bootstrap 5 + Vanilla JS.

## Cấu trúc dự án

```
sunshine-rebuild/
├── package.json                 # npm config & scripts
├── vite.config.js               # Vite build tool config (multi-page)
├── .gitignore
├── .env.example                 # Biến môi trường mẫu
├── README.md
│
├── docs/                        # Tài liệu dự án
│   ├── ANALYSIS.md              #   Phân tích chi tiết site gốc
│   └── BACKEND-REQUIREMENTS.md  #   Yêu cầu backend đầy đủ
│
├── public/                      # Static assets (Vite copy nguyên vẹn)
│   ├── images/                  #   Hình ảnh: logo, cover, marketing, store badges
│   │   └── course-placeholder.svg
│   └── locales/                 #   File ngôn ngữ (runtime fetch)
│       ├── vi.json
│       └── en.json
│
├── index.html                   # Trang chủ (Vite entry)
├── pages/                       # Các trang phụ
│   ├── login.html
│   └── forgot-password.html
│
└── src/                         # Source code
    ├── styles/                  #   CSS — chia module theo chức năng
    │   ├── main.css             #     Entry point (@import tất cả)
    │   ├── base/
    │   │   ├── _variables.css   #     CSS custom properties
    │   │   └── _reset.css       #     Reset & base
    │   ├── layout/
    │   │   ├── _navbar.css      #     Top navigation
    │   │   └── _footer.css      #     Footer
    │   ├── components/
    │   │   ├── _hero.css        #     Hero slider
    │   │   ├── _features.css    #     Marketing features
    │   │   ├── _cards.css       #     Course cards
    │   │   ├── _forms.css       #     Login, forgot password forms
    │   │   └── _accordion.css   #     FAQ accordion
    │   └── responsive/
    │       └── _breakpoints.css #     Media queries
    │
    └── js/                      #   JavaScript — ES Modules
        ├── main.js              #     Entry point (imports all)
        ├── modules/
        │   ├── i18n.js          #     Đa ngôn ngữ vi/en
        │   ├── auth.js          #     Authentication
        │   └── courses.js       #     Course rendering & filter
        ├── data/
        │   └── courseData.js    #     24 khóa học (static data)
        └── utils/
            └── dom.js           #     DOM helpers ($, $$, on)
```

## Công nghệ

- **Vite 6** — Dev server + build tool (HMR, bundling, minify)
- **Bootstrap 5.3.3** — UI framework (npm, không CDN)
- **Font Awesome 6.5.1** — Icons (CDN)
- **Google Fonts** (Roboto) — Typography
- **Vanilla JavaScript** — ES Modules, không framework

## Bắt đầu

```bash
# 1. Cài dependencies
npm install

# 2. Chạy dev server (http://localhost:3000)
npm run dev

# 3. Build production
npm run build

# 4. Preview bản build
npm run preview
```

## npm Scripts

| Script | Mô tả |
|--------|-------|
| `npm run dev` | Chạy Vite dev server với HMR |
| `npm run build` | Build production → thư mục `dist/` |
| `npm run preview` | Preview bản build production |
| `npm run lint:css` | Kiểm tra CSS với Stylelint |
| `npm run lint:js` | Kiểm tra JS với ESLint |
| `npm run lint` | Chạy cả 2 linter |

## Copy hình ảnh từ site gốc

Các file hình ảnh cần copy vào `public/images/`:

```
sunshineschool.edu.vn/pluginfile.php/1/theme_moove/logo/        → public/images/logo.jpg
sunshineschool.edu.vn/pluginfile.php/1/theme_moove/favicon/     → public/images/favicon.ico
sunshineschool.edu.vn/pluginfile.php/1/theme_moove/sliderimage1/ → public/images/cover-website.jpg
sunshineschool.edu.vn/pluginfile.php/1/theme_moove/marketing*icon/ → public/images/marketing1~4.png
```

## Backend

Xem chi tiết tại [docs/BACKEND-REQUIREMENTS.md](docs/BACKEND-REQUIREMENTS.md) bao gồm:

- Kiến trúc hệ thống (Node.js + PostgreSQL + Redis)
- Database schema (11 bảng)
- REST API endpoints (~40+ endpoints)
- Hệ thống Authentication & RBAC (4 roles)
- LMS Core: khóa học, bài tập, quiz
- Quiz Engine: 8 loại câu hỏi + IELTS mock test
- File management, i18n, email/notifications
- Kế hoạch triển khai 4 giai đoạn

## Dữ liệu

- **24 khóa học**: 16 IELTS + 8 CAMBRIDGE (xem `docs/ANALYSIS.md`)
- **Đa ngôn ngữ**: Tiếng Việt (mặc định) + English
- **Liên hệ**: 0935.711.698 | info@sunshinelc.edu.vn
- **Địa chỉ**:
  - VP1: 7 Hoàng Văn Thụ, Hải Châu, Đà Nẵng
  - VP2: 4 Nguyễn Du, Hải Châu, Đà Nẵng
