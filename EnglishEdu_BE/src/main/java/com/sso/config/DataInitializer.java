package com.sso.config;

import com.sso.entity.User;
import com.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Admin account — override via ADMIN_USERNAME / ADMIN_EMAIL / ADMIN_PASSWORD env vars
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@sunshine.local}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    // Set SEED_DEMO_TEACHER=true only in development/test environments
    @Value("${app.seed.demo-teacher:false}")
    private boolean seedDemoTeacher;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("Admin")
                    .lastName("System")
                    .role("ADMIN")
                    .active(true)
                    .guest(false)
                    .build();
            userRepository.save(admin);
            log.info("==============================================");
            log.info("  Admin user created:");
            log.info("  Username : {}", adminUsername);
            log.info("  Email    : {}", adminEmail);
            log.info("  Password : [set via ADMIN_PASSWORD env var]");
            log.info("==============================================");
        }

        // Demo teacher — only created when SEED_DEMO_TEACHER=true (dev/test only)
        if (seedDemoTeacher && userRepository.findByUsername("teacher").isEmpty()) {
            User teacher = User.builder()
                    .username("teacher")
                    .email("teacher@sunshine.local")
                    .password(passwordEncoder.encode("Teacher@123"))
                    .firstName("Giáo Viên")
                    .lastName("Mẫu")
                    .role("TEACHER")
                    .active(true)
                    .guest(false)
                    .build();
            userRepository.save(teacher);
            log.info("Demo teacher user created (username: teacher)");
        }
    }
}

