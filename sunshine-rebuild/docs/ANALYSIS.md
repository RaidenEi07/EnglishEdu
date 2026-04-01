# Phân Tích Website - Sunshine School Online (SSO)

## 1. Tổng Quan

- **Tên**: Sunshine School Online (SSO)
- **Domain gốc**: sunshineschool.edu.vn
- **Nền tảng gốc**: Moodle LMS v4.x với theme Moove
- **Ngôn ngữ**: Song ngữ Việt - Anh (vi/en)
- **Slogan**: "MORE EDUCATION BETTER GENERATION"
- **Copyright**: ©2024 Sunshine. Powered by Videa Edtech.
- **Analytics**: Google Analytics G-SBWXZQB198

---

## 2. Cấu Trúc Trang

### 2.1 Các trang chính
| Trang | Mô tả | File gốc |
|-------|--------|----------|
| Trang chủ (VI) | Homepage tiếng Việt | index.html |
| Trang chủ (EN) | Homepage tiếng Anh | index9ed2.html |
| Đăng nhập | Form đăng nhập | login/index.html |
| Quên mật khẩu | Khôi phục mật khẩu | login/forgot_password.html |
| Data Privacy | Tóm tắt quyền riêng tư | admin/tool/dataprivacy/summary.html |

### 2.2 Bố cục Homepage
1. **Navbar** - Logo, menu, language switcher, nút đăng nhập
2. **Slider** - Banner ảnh "Cover website.jpg"
3. **Marketing Section** - Slogan + 4 feature cards
4. **Course Listing** - 24 khóa học (card-deck, 3 cards/row)
5. **FAQ Section** - Accordion FAQ (placeholder "loremissum")
6. **Footer** - Contact, địa chỉ, thời gian làm việc, app links

---

## 3. Danh Sách Khóa Học (24 khóa học)

### IELTS (14 khóa học)
| ID | Tên khóa học |
|----|-------------|
| 83 | IELTS test preparation platform |
| 84 | Ielts PREP |
| 81 | Joint IELTS Assessment Portal |
| 76 | TIC - MOCKTESTS |
| 68 | PRE - UPPER IELTS |
| 42 | IELTS - INTENSIVE SPEAKING - Luyện Tập 300 Câu Hỏi với ELSA SPEAK |
| 41 | IELTS Mocktest - 30 REAL tests (Listening, Reading) |
| 40 | PRE - INTERMEDIATE |
| 39 | INTER - IELTS |
| 27 | UPPER - IELTS |
| 30 | ADVANCED - IELTS |
| 33 | INTENSIVE - IELTS |
| 82 | Schedule - Session Monitoring |
| 74 | Internal Test |

### CAMBRIDGE (10 khóa học)
| ID | Tên khóa học |
|----|-------------|
| 66 | PREPARE 5 - CAMBRIDGE B1 |
| 38 | PREPARE 4 - CAMBRIDGE B1 |
| 29 | PREPARE 3 - CAMBRIDGE A2 |
| 25 | PREPARE 2 - CAMBRIDGE A2 |
| 73 | Boost your Listening - B2 |
| 72 | Boost your Vocabulary and Reading - B2 |
| 71 | Boost your Listening - A2 |
| 70 | Boost your Vocabulary and Reading - A2 |
| 69 | Boost your Listening - B1 |
| 67 | Boost your Vocabulary and Reading - B1 |

---

## 4. Marketing Features (4 cards)

| # | Tiêu đề | Nội dung |
|---|---------|----------|
| 1 | Chương trình liên kết Giảng Dạy | Đào tạo Cambridge và IELTS cho hơn 1.000 học sinh ở tất cả các cấp độ |
| 2 | Đối tác | Đối tác "Vàng" của IDP Việt Nam với +400 học sinh đạt IELTS 6.5-8.5 |
| 3 | Kinh nghiệm | 5+ năm, hỗ trợ 12 Trung tâm tiếng Anh trên toàn quốc |
| 4 | Chứng chỉ | Đối tác vàng OEA & Language Link, cấp chứng chỉ Cambridge Pre A1-B2 cho 300+ học viên |

---

## 5. Thông Tin Liên Hệ

- **Website**: pbs.sunshineschool.io.vn
- **Điện thoại**: 0979 289 466 / 0985 289 466
- **Trụ sở chính**: K7/210 đường Hoàng Quốc Việt, Q.Cầu Giấy, TP.Hà Nội
- **Cơ sở 1**: 33 Nguyễn Thị Minh Khai, P.Xương Giang, TP.Bắc Giang
- **Cơ sở 2**: Lô 09 đường Pháp Loa, TDP 4, TT.Nham Biền, H.Yên Dũng, T.Bắc Giang

### Thời gian làm việc
- Thứ 3 - Thứ 6: 8AM đến 9:30PM
- Thứ 7, Chủ nhật: 7:30AM đến 7PM
- Thứ 2: Nghỉ

---

## 6. Tài Nguyên Gốc

### Images
- Logo: `pluginfile.php/1/theme_moove/logo/1763353171/898211850cc5a89bf1d4 (1).jpg`
- Favicon: `pluginfile.php/1/theme_moove/favicon/1763353171/favicon.ico`
- Slider: `pluginfile.php/1/theme_moove/sliderimage1/1763353171/Cover website.jpg`
- Marketing icons 1-4: `pluginfile.php/1/theme_moove/marketing[1-4]icon/1763353171/`
- App Store badges: `theme/moove/pix/store_google.svg`, `store_apple.svg`

### CSS
- YUI CSS: `theme/combob48b` (YUI 3.18.1)
- Moove theme styles: `theme/styles.php/moove/1763353171_1/styles.php`
- Google Fonts: Roboto (300, 400, 500, 700)

### JavaScript
- YUI 3.18.1 (Moodle JS framework)
- RequireJS module loader
- jQuery 3.7.1
- MathJax 2.7.9

---

## 7. Tính Năng Chính Cần Rebuild

1. **Authentication**: Login (username/password), guest login, forgot password, language-aware
2. **Course Catalog**: Hiển thị danh sách khóa học với filter theo category
3. **Multilingual**: Song ngữ VI/EN với language switcher
4. **Responsive Design**: Mobile-first với Bootstrap grid
5. **Slider/Banner**: Image carousel
6. **FAQ**: Accordion collapse
7. **Mobile App Links**: Play Store & App Store deep links
8. **SEO**: Meta tags, structured data
