package com.apisentinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:qwe056770@gmail.com}")
    private String senderEmail;

    @Value("${apisentinel.mail.contact-target:jenishraichura58@gmail.com}")
    private String contactTargetEmail;

    // ── OTP ─────────────────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            if (mailSender == null) {
                logger.warn("JavaMailSender not configured. OTP for {}: {}", toEmail, otp);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("ApiSentinel — Your Verification Code");
            message.setText(
                    "Hello,\n\n" +
                            "Your ApiSentinel verification code is:\n\n" +
                            "  " + otp + "\n\n" +
                            "This code expires in 10 minutes. Do not share it with anyone.\n\n" +
                            "If you did not request this, you can safely ignore this email.\n\n" +
                            "Regards,\nApiSentinel");
            mailSender.send(message);
            logger.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email.", e);
        }
    }

    // ── Admin → User message ────────────────────────────────────────────────
    public void sendAdminMessage(String toEmail, String subject, String body) {
        try {
            if (mailSender == null) {
                logger.warn("JavaMailSender not configured. Mocking admin message to {}", toEmail);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("[ApiSentinel Admin] " + subject);
            message.setText(body + "\n\n— ApiSentinel Admin Team");
            mailSender.send(message);
            logger.info("Admin message sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send admin message to {}", toEmail, e);
            throw new RuntimeException("Failed to send admin message.", e);
        }
    }

    // ── Contact form ─────────────────────────────────────────────────────────
    public void sendContactEmail(String name, String fromEmail, String subject, String messageText) {
        try {
            if (mailSender == null) {
                logger.warn("JavaMailSender not configured. Mocking contact email from {}", fromEmail);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(contactTargetEmail);
            message.setReplyTo(fromEmail);
            message.setSubject("ApiSentinel Contact: " + subject);
            message.setText("Name: " + name + "\nEmail: " + fromEmail + "\n\nMessage:\n" + messageText);
            mailSender.send(message);
            logger.info("Contact email sent from {}", fromEmail);
        } catch (Exception e) {
            logger.error("Failed to send contact email: ", e);
            throw new RuntimeException("Failed to send email. Ensure SMTP configuration is valid.", e);
        }
    }

    // ── API Alerts ───────────────────────────────────────────────────────────
    public void sendApiDownAlert(String toEmail, String apiName, int statusCode, String reason) {
        try {
            if (mailSender == null) {
                logger.warn("JavaMailSender not configured. Mocking DOWN alert for {}", apiName);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("🚨 ApiSentinel ALERT: " + apiName + " is DOWN");
            message.setText(
                    "Hello,\n\n" +
                            "Your monitored service '" + apiName + "' has failed 3 consecutive checks.\n\n" +
                            "Status: DOWN\n" +
                            "Error Code: " + statusCode + "\n" +
                            "Explanation: " + reason + "\n" +
                            "Time: " + java.time.LocalDateTime.now() + "\n\n" +
                            "Please check your ApiSentinel dashboard for more details.\n\n" +
                            "Regards,\nApiSentinel Monitoring");
            mailSender.send(message);
            logger.info("Sent DOWN alert email to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send DOWN alert email to {}", toEmail, e);
        }
    }

    public void sendApiRecoveryAlert(String toEmail, String apiName) {
        try {
            if (mailSender == null) {
                logger.warn("JavaMailSender not configured. Mocking RECOVERY alert for {}", apiName);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("✅ ApiSentinel RECOVERY: " + apiName + " is UP");
            message.setText(
                    "Hello,\n\n" +
                            "Good news! Your monitored service '" + apiName
                            + "' has recovered and is responding normally.\n\n" +
                            "Time: " + java.time.LocalDateTime.now() + "\n\n" +
                            "Regards,\nApiSentinel Monitoring");
            mailSender.send(message);
            logger.info("Sent RECOVERY alert email to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send RECOVERY alert email to {}", toEmail, e);
        }
    }
}
