package com.gdut.service.impl;

import com.gdut.common.exception.BusinessException;
import com.gdut.common.enums.ResultCode;
import com.gdut.constant.EmailConstants;
import com.gdut.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendTextEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("文本邮件发送成功: {}", to);
        } catch (Exception e) {
            log.error("文本邮件发送失败: {}", to, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "邮件发送失败");
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("HTML邮件发送成功: {}", to);
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: {}", to, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "邮件发送失败");
        }
    }

    @Override
    public void sendVerificationCodeEmail(String to, String code) {
        // 使用常量中的HTML模板，替换占位符
        String htmlContent = EmailConstants.VERIFICATION_CODE_HTML_TEMPLATE
                .replace(EmailConstants.CODE_PLACEHOLDER, code);
        
        sendHtmlEmail(to, EmailConstants.VERIFICATION_CODE_SUBJECT, htmlContent);
    }
}
