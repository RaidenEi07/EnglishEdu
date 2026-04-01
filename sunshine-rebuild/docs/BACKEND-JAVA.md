# Kế hoạch Backend Java – Sunshine School Online (SSO)

> Phân tích từ frontend Vite + 10 API endpoints đang được gọi + 24 courses + Moodle LMS logic  
> Ngày lập: 2026-03-16

---

## 1. CÔNG NGHỆ SỬ DỤNG

| Layer | Công nghệ | Lý do chọn |
|---|---|---|
| Language | **Java 21** (LTS) | Virtual threads, records, pattern matching |
| Framework | **Spring Boot 3.3.x** | De facto standard, ecosystem hoàn chỉnh |
| Security | **Spring Security 6 + JWT** | Stateless auth, phù hợp với SPA |
| ORM | **Spring Data JPA + Hibernate** | Mature, đủ mạnh cho LMS |
| Database | **PostgreSQL 16** | ACID, JSON support, fulltext search |
| Cache | **Redis 7** | Session blacklist JWT, rate-limit |
| Migration | **Flyway** | Version-controlled SQL migrations |
| Mapping | **MapStruct 1.6** | Compile-time DTO mapping, zero reflection |
| Boilerplate | **Lombok** | Giảm code thừa |
| API Docs | **SpringDoc OpenAPI 2.x** | Swagger UI tự động |
| Mail | **Spring Boot Starter Mail** | Forgot password email |
| File Storage | **MinIO** (self-host) hoặc **AWS S3** | Avatar & course images |
| Build | **Maven 3.9** | Dependency management |

---

## 2. DEPENDENCIES – pom.xml đầy đủ

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- ═══════════════════════════════ PARENT ═══════════════ -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.3</version>
        <relativePath/>
    </parent>

    <groupId>com.sso</groupId>
    <artifactId>sso-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>SSO Backend</name>
    <description>Sunshine School Online – Backend API</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <mapstruct.version>1.6.2</mapstruct.version>
        <springdoc.version>2.8.6</springdoc.version>   <!-- 2.8+ tương thích Spring Boot 4 -->
        <minio.version>8.5.12</minio.version>
    </properties>

    <dependencies>

        <!-- ═══════════════════════════ SPRING BOOT CORE ════════ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <!-- ═══════════════════════════ JWT ═════════════════════ -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- ═══════════════════════════ DATABASE ════════════════ -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- ═══════════════════════════ UTILITIES ═══════════════ -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- ═══════════════════════════ API DOCS ════════════════ -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- ═══════════════════════════ FILE STORAGE ════════════ -->
        <!-- Option A: MinIO (self-hosted) -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>

        <!-- Option B: AWS S3 (thay thế MinIO nếu dùng cloud) -->
        <!--
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.28.0</version>
        </dependency>
        -->

        <!-- ═══════════════════════════ TESTING ═════════════════ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 3. CẤU TRÚC THƯ MỤC DỰ ÁN

```
sso-backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/sso/
    │   │   ├── SsoApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java          # JWT filter chain, CORS, public endpoints
    │   │   │   ├── JwtProperties.java            # @ConfigurationProperties jwt.*
    │   │   │   ├── RedisConfig.java              # RedisTemplate + cache manager
    │   │   │   ├── OpenApiConfig.java            # Swagger UI + Bearer auth scheme
    │   │   │   └── StorageConfig.java            # MinIO / S3 client bean
    │   │   │
    │   │   ├── controller/                       # @RestController, @RequestMapping("/api/v1/...")
    │   │   │   ├── AuthController.java           # /auth/*
    │   │   │   ├── UserController.java           # /users/me
    │   │   │   ├── CourseController.java         # /courses/* , /courses/enrolled, /courses/recent
    │   │   │   ├── TimelineController.java       # /timeline
    │   │   │   ├── NotificationController.java   # /notifications
    │   │   │   └── CalendarController.java       # /calendar
    │   │   │
    │   │   ├── service/
    │   │   │   ├── AuthService.java
    │   │   │   ├── UserService.java
    │   │   │   ├── CourseService.java
    │   │   │   ├── EnrollmentService.java
    │   │   │   ├── TimelineService.java
    │   │   │   ├── NotificationService.java
    │   │   │   ├── EmailService.java             # JavaMailSender
    │   │   │   └── StorageService.java           # MinIO upload/delete
    │   │   │
    │   │   ├── repository/                       # Spring Data JPA interfaces
    │   │   │   ├── UserRepository.java
    │   │   │   ├── CourseRepository.java
    │   │   │   ├── EnrollmentRepository.java
    │   │   │   ├── TimelineEventRepository.java
    │   │   │   ├── NotificationRepository.java
    │   │   │   ├── CalendarEventRepository.java
    │   │   │   └── PasswordResetTokenRepository.java
    │   │   │
    │   │   ├── entity/                           # @Entity JPA classes
    │   │   │   ├── User.java
    │   │   │   ├── Course.java
    │   │   │   ├── Enrollment.java               # user ↔ course (join table + extra fields)
    │   │   │   ├── TimelineEvent.java
    │   │   │   ├── Notification.java
    │   │   │   ├── CalendarEvent.java
    │   │   │   └── PasswordResetToken.java
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── ForgotPasswordRequest.java
    │   │   │   │   ├── UpdateProfileRequest.java
    │   │   │   │   └── ChangePasswordRequest.java
    │   │   │   └── response/
    │   │   │       ├── ApiResponse.java          # {success, message, data}
    │   │   │       ├── AuthResponse.java         # {token, user}
    │   │   │       ├── UserResponse.java
    │   │   │       ├── CourseResponse.java
    │   │   │       ├── EnrolledCourseResponse.java
    │   │   │       └── TimelineEventResponse.java
    │   │   │
    │   │   ├── security/
    │   │   │   ├── JwtTokenProvider.java         # generate / validate / parse JWT
    │   │   │   ├── JwtAuthenticationFilter.java  # OncePerRequestFilter
    │   │   │   └── UserDetailsServiceImpl.java   # loadUserByUsername
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── UnauthorizedException.java
    │   │   │   └── BadRequestException.java
    │   │   │
    │   │   └── util/
    │   │       ├── AuthUtils.java                # get current user from SecurityContext
    │   │       └── SlugUtils.java
    │   │
    │   └── resources/
    │       ├── application.yml                   # base config
    │       ├── application-dev.yml               # local dev overrides
    │       ├── application-prod.yml              # production overrides
    │       └── db/migration/
    │           ├── V1__create_users.sql
    │           ├── V2__create_courses.sql
    │           ├── V3__create_enrollments.sql
    │           ├── V4__create_timeline_events.sql
    │           ├── V5__create_notifications.sql
    │           ├── V6__create_calendar_events.sql
    │           ├── V7__create_password_reset_tokens.sql
    │           └── V8__seed_courses.sql          # 24 courses từ courseData.js
    │
    └── test/
        └── java/com/sso/
            ├── controller/
            │   ├── AuthControllerTest.java
            │   └── CourseControllerTest.java
            └── service/
                ├── AuthServiceTest.java
                └── CourseServiceTest.java
```

---

## 4. DATABASE SCHEMA

### 4.1 Bảng `users`
```sql
CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(100) UNIQUE NOT NULL,
    email        VARCHAR(255) UNIQUE NOT NULL,
    password     VARCHAR(255) NOT NULL,         -- BCrypt hash
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    avatar_url   VARCHAR(500),
    role         VARCHAR(20) DEFAULT 'STUDENT', -- STUDENT | TEACHER | ADMIN
    is_active    BOOLEAN DEFAULT TRUE,
    is_guest     BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);
```

### 4.2 Bảng `courses`
```sql
CREATE TABLE courses (
    id           BIGSERIAL PRIMARY KEY,
    external_id  INT UNIQUE,                    -- ID từ Moodle (83, 84, 40...)
    name         VARCHAR(500) NOT NULL,
    category     VARCHAR(50),                   -- IELTS | CAMBRIDGE
    level        VARCHAR(100),
    description  TEXT,
    image_url    VARCHAR(500),
    is_published BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
```

### 4.3 Bảng `enrollments` (user ↔ course)
```sql
CREATE TABLE enrollments (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id      BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status         VARCHAR(20) DEFAULT 'inprogress', -- inprogress | past | future | starred | hidden
    progress       INT DEFAULT 0,                    -- 0–100 (%)
    enrolled_at    TIMESTAMPTZ DEFAULT NOW(),
    last_accessed  TIMESTAMPTZ,
    UNIQUE(user_id, course_id)
);
```

### 4.4 Bảng `timeline_events`
```sql
CREATE TABLE timeline_events (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT REFERENCES users(id) ON DELETE CASCADE,
    course_id    BIGINT REFERENCES courses(id) ON DELETE SET NULL,
    title        VARCHAR(500) NOT NULL,
    type         VARCHAR(50),                   -- assignment | quiz | lesson
    due_at       TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);
```

### 4.5 Bảng `notifications`
```sql
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message    TEXT NOT NULL,
    link       VARCHAR(500),
    is_read    BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### 4.6 Bảng `calendar_events`
```sql
CREATE TABLE calendar_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(300) NOT NULL,
    description TEXT,
    event_date  DATE NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

### 4.7 Bảng `password_reset_tokens`
```sql
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 5. API ENDPOINTS ĐẦY ĐỦ

Tất cả endpoints có prefix `/api/v1`. Các endpoint có 🔒 yêu cầu `Authorization: Bearer <token>`.

### 5.1 Auth — `/api/v1/auth`

| Method | Endpoint | Body | Response | Mô tả |
|--------|----------|------|----------|-------|
| POST | `/auth/login` | `{username, password}` | `{token, user}` | Đăng nhập |
| POST | `/auth/guest` | – | `{token}` | Đăng nhập khách |
| POST | `/auth/forgot-password` | `{search}` | `{message}` | Gửi email reset |
| POST | `/auth/logout` 🔒 | – | 204 | Blacklist token Redis |

### 5.2 Users — `/api/v1/users`

| Method | Endpoint | Body | Response | Mô tả |
|--------|----------|------|----------|-------|
| GET | `/users/me` 🔒 | – | `UserResponse` | Lấy thông tin profile |
| PUT | `/users/me` 🔒 | `{firstName, lastName, email}` | `UserResponse` | Cập nhật profile |
| POST | `/users/me/password` 🔒 | `{currentPassword, newPassword}` | `{message}` | Đổi mật khẩu |
| POST | `/users/me/avatar` 🔒 | `multipart/form-data (file)` | `UserResponse` | Upload avatar |

### 5.3 Courses — `/api/v1/courses`

| Method | Endpoint | Params | Response | Mô tả |
|--------|----------|--------|----------|-------|
| GET | `/courses` | `?category=IELTS&page=0&size=20` | `Page<CourseResponse>` | Danh sách khoá học công khai |
| GET | `/courses/{id}` | – | `CourseResponse` | Chi tiết khoá học |
| GET | `/courses/enrolled` 🔒 | `?status=all&sort=title&page=0` | `List<EnrolledCourseResponse>` | Các khoá học đã đăng ký |
| GET | `/courses/recent` 🔒 | `?limit=10` | `List<EnrolledCourseResponse>` | Truy cập gần đây |
| POST | `/courses/{id}/enroll` 🔒 | – | `EnrolledCourseResponse` | Đăng ký khoá học |
| PATCH | `/courses/{id}/enrollment` 🔒 | `{status, progress}` | `EnrolledCourseResponse` | Cập nhật trạng thái/tiến độ |

### 5.4 Timeline — `/api/v1/timeline`

| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/timeline` 🔒 | `?days=7&sort=dates` | `List<TimelineEventResponse>` |

### 5.5 Notifications — `/api/v1/notifications`

| Method | Endpoint | Response |
|--------|----------|----------|
| GET | `/notifications` 🔒 | `List<NotificationResponse>` |
| PUT | `/notifications/read-all` 🔒 | 204 |
| DELETE | `/notifications/{id}` 🔒 | 204 |

### 5.6 Calendar — `/api/v1/calendar`

| Method | Endpoint | Params/Body | Response |
|--------|----------|-------------|----------|
| GET | `/calendar` 🔒 | `?year=2026&month=3` | `List<CalendarEventResponse>` |
| POST | `/calendar` 🔒 | `{title, eventDate, description}` | `CalendarEventResponse` |
| DELETE | `/calendar/{id}` 🔒 | – | 204 |

---

## 6. LUỒNG AUTHENTICATION (JWT)

```
Frontend                          Backend
───────                           ───────
POST /auth/login ─────────────→  AuthController.login()
  {username, password}              └─ UserDetailsService.loadUserByUsername()
                                    └─ BCrypt.matches(password, hash)
                                    └─ JwtTokenProvider.generateToken(user)
←──────────────────────────────  {token: "eyJ...", user: {...}}

localStorage.setItem('sso_token')

GET /courses/enrolled ─────────→  JwtAuthenticationFilter (OncePerRequestFilter)
  Authorization: Bearer eyJ...       └─ JwtTokenProvider.validateToken()
                                      └─ SecurityContext.setAuthentication()
                                    CourseController.getEnrolled()
←──────────────────────────────  [{id, name, progress, ...}, ...]
```

**JWT payload:**
```json
{
  "sub": "1",
  "username": "student01",
  "role": "STUDENT",
  "iat": 1742000000,
  "exp": 1742086400
}
```

**Token TTL:** Access token = 24h, lưu trong Redis để blacklist khi logout.

---

## 7. CẤU HÌNH application.yml

```yaml
server:
  port: 4000

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sso_db
    username: sso_user
    password: sso_pass
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate          # Flyway quản lý schema
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration

  data:
    redis:
      host: localhost
      port: 6379

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

jwt:
  secret: ${JWT_SECRET}           # >= 256 bit, lưu qua env var
  expiration: 86400000            # 24h ms

minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: sso-uploads

cors:
  allowed-origins:
    - http://localhost:3000
    - https://sunshineschool.edu.vn
```

---

## 8. SECURITY CONFIG (outline)

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/courses",
                    "/api/v1/courses/{id}",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 9. RESPONSE CHUẨN

Mọi API đều trả về cấu trúc nhất quán:

```json
// Thành công
{
  "success": true,
  "message": "OK",
  "data": { ... }
}

// Lỗi
{
  "success": false,
  "message": "Tên đăng nhập hoặc mật khẩu không đúng",
  "errors": ["field: message"]
}
```

---

## 10. LỘ TRÌNH TRIỂN KHAI (theo thứ tự ưu tiên)

### Giai đoạn 1 — Core Auth (1–2 ngày)
- [ ] Init Spring Boot project, pom.xml
- [ ] Cấu hình PostgreSQL + Flyway migration V1 (users table)
- [ ] `User` entity, `UserRepository`, `UserDetailsServiceImpl`
- [ ] `JwtTokenProvider` – generate / validate token
- [ ] `JwtAuthenticationFilter`
- [ ] `SecurityConfig` – CORS + filter chain
- [ ] `AuthController` – POST /auth/login, /auth/guest, /auth/forgot-password
- [ ] `EmailService` – gửi email reset password

### Giai đoạn 2 — User Profile (1 ngày)
- [ ] `UserController` – GET/PUT /users/me, POST /password, POST /avatar
- [ ] `StorageService` – MinIO upload avatar
- [ ] `UserMapper` (MapStruct)

### Giai đoạn 3 — Courses & Enrollments (2 ngày)
- [ ] Flyway V2–V3 (courses, enrollments)
- [ ] Seed V8 – 24 courses từ `courseData.js`
- [ ] `CourseController` – GET /courses, /courses/{id}
- [ ] `CourseController` – GET /courses/enrolled, /courses/recent
- [ ] PATCH /courses/{id}/enrollment (cập nhật progress, status)

### Giai đoạn 4 — Dashboard Features (1 ngày)
- [ ] Flyway V4–V6 (timeline_events, notifications, calendar_events)
- [ ] `TimelineController` – GET /timeline
- [ ] `NotificationController` – GET/PUT /notifications
- [ ] `CalendarController` – GET/POST/DELETE /calendar

### Giai đoạn 5 — Polish & Deploy
- [ ] `GlobalExceptionHandler` – chuẩn hoá error response
- [ ] Rate limiting (Redis) cho /auth/login
- [ ] Swagger UI config
- [ ] Unit tests Controller + Service
- [ ] Docker Compose (postgres + redis + minio + app)
- [ ] CI/CD pipeline

---

## 11. DOCKER COMPOSE (dev environment)

```yaml
version: '3.9'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: sso_db
      POSTGRES_USER: sso_user
      POSTGRES_PASSWORD: sso_pass
    ports: ["5432:5432"]
    volumes: [postgres_data:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports: ["9000:9000", "9001:9001"]
    volumes: [minio_data:/data]

volumes:
  postgres_data:
  minio_data:
```

Khởi động: `docker compose up -d`  
Backend chạy: `mvn spring-boot:run` (port 4000)  
Frontend chạy: `npm run dev` (port 3000)

---

## 12. TÓM TẮT DEPENDENCY THEO NHÓM

| Nhóm | Dependency | Version |
|---|---|---|
| Core | spring-boot-starter-web | 3.3.5 (từ parent) |
| Security | spring-boot-starter-security | 3.3.5 |
| JWT | jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 |
| Database | spring-boot-starter-data-jpa | 3.3.5 |
| Database | postgresql (driver) | runtime |
| Migration | flyway-core + flyway-database-postgresql | 10.x |
| Cache | spring-boot-starter-data-redis | 3.3.5 |
| Validation | spring-boot-starter-validation | 3.3.5 |
| Email | spring-boot-starter-mail | 3.3.5 |
| Mapping | mapstruct + mapstruct-processor | 1.6.2 |
| Boilerplate | lombok | 1.18.x |
| API Docs | springdoc-openapi-starter-webmvc-ui | 2.6.0 |
| File Storage | minio | 8.5.12 |
| Testing | spring-boot-starter-test + spring-security-test | 3.3.5 |
| Testing | h2 (in-memory, scope test) | built-in |
