package com.smartcampus.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String from;

    @Async
    public void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }

    public void sendRegisterCode(String to, String code) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;border:1px solid #e5e7eb;border-radius:12px'>"
                + "<h2 style='color:#0d6efd;margin-bottom:8px'>地点探索与分享</h2>"
                + "<p style='color:#374151;font-size:15px'>您正在注册账号，邮箱验证码为：</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;color:#0d6efd;margin:20px 0'>" + code + "</div>"
                + "<p style='color:#6b7280;font-size:13px'>验证码 5 分钟内有效，请勿泄露给他人。</p>"
                + "</div>";
        sendHtml(to, "【地点探索与分享】注册验证码", html);
    }

    public void sendPasswordResetCode(String to, String code) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;border:1px solid #e5e7eb;border-radius:12px'>"
                + "<h2 style='color:#0d6efd;margin-bottom:8px'>地点探索与分享</h2>"
                + "<p style='color:#374151;font-size:15px'>您正在重置密码，验证码为：</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;color:#0d6efd;margin:20px 0'>" + code + "</div>"
                + "<p style='color:#6b7280;font-size:13px'>验证码 5 分钟内有效，请勿泄露给他人。如非本人操作，请忽略此邮件。</p>"
                + "</div>";
        sendHtml(to, "【地点探索与分享】重置密码验证码", html);
    }

    public void sendBindCode(String to, String code) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;border:1px solid #e5e7eb;border-radius:12px'>"
                + "<h2 style='color:#0d6efd;margin-bottom:8px'>地点探索与分享</h2>"
                + "<p style='color:#374151;font-size:15px'>您正在为账号绑定邮箱，验证码为：</p>"
                + "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;color:#0d6efd;margin:20px 0'>" + code + "</div>"
                + "<p style='color:#6b7280;font-size:13px'>验证码 5 分钟内有效，请勿泄露给他人。如非本人操作，请忽略此邮件。</p>"
                + "</div>";
        sendHtml(to, "【地点探索与分享】绑定邮箱验证码", html);
    }

    /** 账号注销确认：注销后用户已无法登录站内，此邮件是唯一的主动通知渠道 */
    public void sendAccountDeletionNotice(String to) {
        String html = "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;border:1px solid #e5e7eb;border-radius:12px'>"
                + "<h2 style='color:#0d6efd;margin-bottom:8px'>地点探索与分享</h2>"
                + "<p style='color:#374151;font-size:15px'>您的账号已成功注销。</p>"
                + "<p style='color:#374151;font-size:15px'>账号信息已匿名化处理：用户名、昵称、个人简介与头像已被清除，您发布的内容将以「已注销用户」身份保留展示。</p>"
                + "<p style='color:#374151;font-size:15px'>该操作不可恢复。如需继续使用，请重新注册账号。</p>"
                + "<p style='color:#6b7280;font-size:13px'>如非本人操作，请尽快联系管理员。</p>"
                + "</div>";
        sendHtml(to, "【地点探索与分享】账号注销确认", html);
    }
}
