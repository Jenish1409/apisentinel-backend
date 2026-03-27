package com.apisentinel.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${APISENTINEL_MAIL_FROM:}")
    private String fromEmail;

    @Value("${apisentinel.mail.contact-target:jenishraichura58@gmail.com}")
    private String contactTargetEmail;

    private String resolveFromEmail() {
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail;
        }
        return Objects.requireNonNullElse(smtpUsername, "");
    }

    // ─── Shared HTML wrapper ────────────────────────────────────────────────
    private String wrapInTemplate(String bodyContent) {
        return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>" +
            "<body style=\"margin:0;padding:0;background-color:#f1f5f9;font-family:'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;\">" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f1f5f9;padding:40px 20px;\">" +
            "<tr><td align=\"center\">" +
            "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);\">" +
            // Header bar
            "<tr><td style=\"background:linear-gradient(135deg,#4f46e5,#3b82f6);padding:28px 36px;\">" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
            "<td><span style=\"font-size:22px;font-weight:700;color:#ffffff;letter-spacing:-0.3px;\">ApiSentinel</span></td>" +
            "<td align=\"right\"><span style=\"font-size:12px;color:rgba(255,255,255,0.7);font-weight:500;\">Monitoring Platform</span></td>" +
            "</tr></table></td></tr>" +
            // Body content area
            "<tr><td style=\"padding:36px;\">" +
            bodyContent +
            "</td></tr>" +
            // Footer
            "<tr><td style=\"background-color:#f8fafc;padding:20px 36px;border-top:1px solid #e2e8f0;\">" +
            "<p style=\"margin:0;font-size:12px;color:#94a3b8;text-align:center;\">" +
            "&copy; " + LocalDateTime.now().getYear() + " ApiSentinel &middot; Reliable API Monitoring" +
            "</p></td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        if (mailSender == null) {
            logger.warn("JavaMailSender not configured. Subject: {}, To: {}", subject, to);
            return;
        }
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(resolveFromEmail());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(mimeMessage);
    }

    // ── OTP ─────────────────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            String body =
                "<h2 style=\"margin:0 0 8px;font-size:20px;font-weight:700;color:#1e293b;\">Verify Your Email</h2>" +
                "<p style=\"margin:0 0 24px;font-size:14px;color:#64748b;line-height:1.6;\">Enter this 6-digit code to complete your ApiSentinel registration. The code is valid for <strong>10 minutes</strong>.</p>" +
                // OTP Code box
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">" +
                "<tr><td align=\"center\">" +
                "<div style=\"display:inline-block;background:linear-gradient(135deg,#eef2ff,#e0e7ff);border:2px solid #c7d2fe;border-radius:12px;padding:20px 48px;\">" +
                "<span style=\"font-size:36px;font-weight:800;letter-spacing:12px;color:#4f46e5;font-family:'Courier New',monospace;\">" + otp + "</span>" +
                "</div>" +
                "</td></tr></table>" +
                // Security notice
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr><td style=\"background-color:#fffbeb;border-left:4px solid #f59e0b;border-radius:0 8px 8px 0;padding:12px 16px;\">" +
                "<p style=\"margin:0;font-size:13px;color:#92400e;line-height:1.5;\">" +
                "<strong>Security Notice:</strong> Never share this code with anyone. ApiSentinel staff will never ask for it." +
                "</p></td></tr></table>" +
                "<p style=\"margin:24px 0 0;font-size:13px;color:#94a3b8;line-height:1.5;\">If you didn't request this verification, you can safely ignore this email.</p>";

            sendHtmlEmail(toEmail, "ApiSentinel — Your Verification Code", wrapInTemplate(body));
            logger.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email.", e);
        }
    }

    // ── Admin → User message ────────────────────────────────────────────────
    public void sendAdminMessage(String toEmail, String subject, String messageBody) {
        try {
            String body =
                "<h2 style=\"margin:0 0 8px;font-size:20px;font-weight:700;color:#1e293b;\">Message from Admin</h2>" +
                "<p style=\"margin:0 0 24px;font-size:14px;color:#64748b;\">You have a new message from the ApiSentinel team.</p>" +
                // Subject badge
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:16px;\">" +
                "<tr><td>" +
                "<span style=\"display:inline-block;background-color:#f1f5f9;color:#475569;font-size:12px;font-weight:600;padding:6px 14px;border-radius:20px;text-transform:uppercase;letter-spacing:0.5px;\">RE: " + escapeHtml(subject) + "</span>" +
                "</td></tr></table>" +
                // Message card
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr><td style=\"background-color:#f8fafc;border-radius:12px;padding:20px 24px;border:1px solid #e2e8f0;\">" +
                "<p style=\"margin:0;font-size:14px;color:#334155;line-height:1.7;white-space:pre-line;\">" + escapeHtml(messageBody) + "</p>" +
                "</td></tr></table>" +
                "<p style=\"margin:20px 0 0;font-size:13px;color:#94a3b8;\">— ApiSentinel Admin Team</p>";

            sendHtmlEmail(toEmail, "[ApiSentinel Admin] " + subject, wrapInTemplate(body));
            logger.info("Admin message sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send admin message to {}", toEmail, e);
            throw new RuntimeException("Failed to send admin message.", e);
        }
    }

    // ── Contact form ─────────────────────────────────────────────────────────
    public void sendContactEmail(String name, String senderEmail, String subject, String messageText) {
        try {
            if (mailSender == null) {
                logger.warn("JavaMailSender not configured. Mocking contact email from {}", senderEmail);
                return;
            }
            String body =
                "<h2 style=\"margin:0 0 8px;font-size:20px;font-weight:700;color:#1e293b;\">New Contact Inquiry</h2>" +
                "<p style=\"margin:0 0 24px;font-size:14px;color:#64748b;\">You received a message via the ApiSentinel contact form.</p>" +
                // Info cards
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:20px;\">" +
                "<tr>" +
                "<td width=\"50%\" style=\"padding-right:8px;\">" +
                "<div style=\"background-color:#f1f5f9;border-radius:10px;padding:14px 18px;\">" +
                "<p style=\"margin:0 0 2px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;\">From</p>" +
                "<p style=\"margin:0;font-size:14px;color:#1e293b;font-weight:600;\">" + escapeHtml(name) + "</p>" +
                "</div></td>" +
                "<td width=\"50%\" style=\"padding-left:8px;\">" +
                "<div style=\"background-color:#f1f5f9;border-radius:10px;padding:14px 18px;\">" +
                "<p style=\"margin:0 0 2px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;\">Email</p>" +
                "<p style=\"margin:0;font-size:14px;color:#1e293b;font-weight:600;\">" + escapeHtml(senderEmail) + "</p>" +
                "</div></td>" +
                "</tr></table>" +
                // Subject
                "<p style=\"margin:0 0 8px;font-size:12px;color:#94a3b8;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;\">Subject</p>" +
                "<p style=\"margin:0 0 16px;font-size:15px;color:#1e293b;font-weight:600;\">" + escapeHtml(subject) + "</p>" +
                // Message body
                "<p style=\"margin:0 0 8px;font-size:12px;color:#94a3b8;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;\">Message</p>" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr><td style=\"background-color:#f8fafc;border-radius:12px;padding:20px 24px;border:1px solid #e2e8f0;\">" +
                "<p style=\"margin:0;font-size:14px;color:#334155;line-height:1.7;white-space:pre-line;\">" + escapeHtml(messageText) + "</p>" +
                "</td></tr></table>";

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(resolveFromEmail());
            helper.setTo(contactTargetEmail);
            helper.setReplyTo(senderEmail);
            helper.setSubject("ApiSentinel Contact: " + subject);
            helper.setText(wrapInTemplate(body), true);
            mailSender.send(mimeMessage);
            logger.info("Contact email sent from {}", senderEmail);
        } catch (Exception e) {
            logger.error("Failed to send contact email: ", e);
            throw new RuntimeException("Failed to send email. Ensure SMTP configuration is valid.", e);
        }
    }

    // ── API Down Alert ──────────────────────────────────────────────────────
    public void sendApiDownAlert(String toEmail, String apiName, int statusCode, String reason) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
            String body =
                // Alert banner
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">" +
                "<tr><td style=\"background:linear-gradient(135deg,#fef2f2,#fee2e2);border-radius:12px;padding:20px 24px;border:1px solid #fecaca;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
                "<td width=\"44\" valign=\"top\"><div style=\"width:40px;height:40px;background-color:#ef4444;border-radius:10px;text-align:center;line-height:40px;font-size:20px;\">&#x26A0;</div></td>" +
                "<td style=\"padding-left:14px;\">" +
                "<h2 style=\"margin:0 0 4px;font-size:18px;font-weight:700;color:#991b1b;\">Service Down Detected</h2>" +
                "<p style=\"margin:0;font-size:13px;color:#b91c1c;\">" + escapeHtml(apiName) + " has failed 3 consecutive checks</p>" +
                "</td></tr></table></td></tr></table>" +
                // Details grid
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">" +
                "<tr>" +
                "<td width=\"33%\" style=\"padding-right:6px;\">" +
                "<div style=\"background-color:#fef2f2;border-radius:10px;padding:16px;text-align:center;\">" +
                "<p style=\"margin:0 0 4px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;\">Status</p>" +
                "<p style=\"margin:0;font-size:16px;font-weight:700;color:#ef4444;\">DOWN</p>" +
                "</div></td>" +
                "<td width=\"33%\" style=\"padding:0 3px;\">" +
                "<div style=\"background-color:#f1f5f9;border-radius:10px;padding:16px;text-align:center;\">" +
                "<p style=\"margin:0 0 4px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;\">Error Code</p>" +
                "<p style=\"margin:0;font-size:16px;font-weight:700;color:#1e293b;\">" + statusCode + "</p>" +
                "</div></td>" +
                "<td width=\"33%\" style=\"padding-left:6px;\">" +
                "<div style=\"background-color:#f1f5f9;border-radius:10px;padding:16px;text-align:center;\">" +
                "<p style=\"margin:0 0 4px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;\">Detected</p>" +
                "<p style=\"margin:0;font-size:12px;font-weight:600;color:#1e293b;\">" + timestamp + "</p>" +
                "</div></td>" +
                "</tr></table>" +
                // Reason
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:20px;\">" +
                "<tr><td style=\"background-color:#f8fafc;border-left:4px solid #ef4444;border-radius:0 8px 8px 0;padding:14px 18px;\">" +
                "<p style=\"margin:0 0 2px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;\">Reason</p>" +
                "<p style=\"margin:0;font-size:14px;color:#334155;font-weight:500;\">" + escapeHtml(reason) + "</p>" +
                "</td></tr></table>" +
                "<p style=\"margin:0;font-size:13px;color:#94a3b8;line-height:1.5;\">Check your ApiSentinel dashboard for detailed logs and history.</p>";

            sendHtmlEmail(toEmail, "🚨 ApiSentinel ALERT: " + apiName + " is DOWN", wrapInTemplate(body));
            logger.info("Sent DOWN alert email to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send DOWN alert email to {}", toEmail, e);
        }
    }

    // ── API Recovery Alert ──────────────────────────────────────────────────
    public void sendApiRecoveryAlert(String toEmail, String apiName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
            String body =
                // Recovery banner
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">" +
                "<tr><td style=\"background:linear-gradient(135deg,#f0fdf4,#dcfce7);border-radius:12px;padding:20px 24px;border:1px solid #bbf7d0;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>" +
                "<td width=\"44\" valign=\"top\"><div style=\"width:40px;height:40px;background-color:#22c55e;border-radius:10px;text-align:center;line-height:40px;font-size:20px;color:#ffffff;\">&#10003;</div></td>" +
                "<td style=\"padding-left:14px;\">" +
                "<h2 style=\"margin:0 0 4px;font-size:18px;font-weight:700;color:#166534;\">Service Recovered</h2>" +
                "<p style=\"margin:0;font-size:13px;color:#15803d;\">" + escapeHtml(apiName) + " is back online and responding normally</p>" +
                "</td></tr></table></td></tr></table>" +
                // Details
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:24px;\">" +
                "<tr>" +
                "<td width=\"50%\" style=\"padding-right:6px;\">" +
                "<div style=\"background-color:#f0fdf4;border-radius:10px;padding:16px;text-align:center;\">" +
                "<p style=\"margin:0 0 4px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;\">Status</p>" +
                "<p style=\"margin:0;font-size:16px;font-weight:700;color:#22c55e;\">UP</p>" +
                "</div></td>" +
                "<td width=\"50%\" style=\"padding-left:6px;\">" +
                "<div style=\"background-color:#f1f5f9;border-radius:10px;padding:16px;text-align:center;\">" +
                "<p style=\"margin:0 0 4px;font-size:11px;color:#94a3b8;font-weight:600;text-transform:uppercase;\">Recovered At</p>" +
                "<p style=\"margin:0;font-size:13px;font-weight:600;color:#1e293b;\">" + timestamp + "</p>" +
                "</div></td>" +
                "</tr></table>" +
                "<p style=\"margin:0;font-size:13px;color:#94a3b8;line-height:1.5;\">Your service has been automatically restored and monitoring continues as normal.</p>";

            sendHtmlEmail(toEmail, "✅ ApiSentinel RECOVERY: " + apiName + " is UP", wrapInTemplate(body));
            logger.info("Sent RECOVERY alert email to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send RECOVERY alert email to {}", toEmail, e);
        }
    }

    // ─── HTML escaping utility ──────────────────────────────────────────────
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
