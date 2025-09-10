package com.otorael.BaseAuths.service.NotificationService;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class EmailMessaging {

    private final JavaMailSender mailSender;

    public EmailMessaging(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     *
     * @param toEmail That receive otp
     * @param resetToken OTP
     */

    @Async
    public void sendOtpEmailAsync(String toEmail, String resetToken) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Quickripto Password Reset Code");

        message.setText(
                "Hello,\n\n"
                        + "We received a request to reset your Quickripto account password.\n"
                        + "Use the one-time password (OTP) provided below to proceed with resetting your password:\n\n"
                        + "OTP Code: " + resetToken + "\n\n"
                        + "⚠️ Please note:\n"
                        + "• This OTP is valid for the next 10 minutes only.\n"
                        + "• If you did not request this password reset, you can safely ignore this email.\n\n"
                        + "🔗 Explore Our Latest Project:\n"
                        + "https://voltech-secure.onrender.com\n\n"
                        + "#CyberSecurity | #ICTConsultancy | #SoftwareDevelopment\n\n"
                        + "Kind regards,\n"
                        + "The Quickripto Team\n"
                        + "-------------------------------------------\n"
                        + "📧 support@quickripto.com | 🌐 www.quickripto.com\n"
                        + "© " + java.time.Year.now() + " Quickripto. All rights reserved."
        );

        mailSender.send(message);
    }
}
