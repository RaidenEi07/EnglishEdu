package com.sso.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset - Sunshine School Online");
        message.setText("Use the following link to reset your password:\n\n"
                + baseUrl + "/pages/forgot-password/?token=" + token
                + "\n\nThis link expires in 1 hour.");
        mailSender.send(message);
    }
}
