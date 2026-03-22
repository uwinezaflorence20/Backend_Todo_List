package com.example.todo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - Todo App");

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f9;">
                    <div style="max-width: 600px; margin: 40px auto; background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #3498db, #2c3e50); padding: 30px; text-align: center;">
                            <h1 style="color: white; margin: 0;">🔐 Password Reset</h1>
                        </div>
                        <div style="padding: 30px;">
                            <p style="color: #2c3e50; font-size: 16px;">Hello,</p>
                            <p style="color: #7f8c8d; font-size: 14px;">We received a request to reset your password. Click the button below to set a new password:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s" style="display: inline-block; padding: 14px 28px; background-color: #3498db; color: white; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;">Reset Password</a>
                            </div>
                            <p style="color: #7f8c8d; font-size: 13px;">This link will expire in <strong>30 minutes</strong>.</p>
                            <p style="color: #7f8c8d; font-size: 13px;">If you didn't request this, please ignore this email.</p>
                            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                            <p style="color: #bdc3c7; font-size: 12px; text-align: center;">Todo App &copy; 2026</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
