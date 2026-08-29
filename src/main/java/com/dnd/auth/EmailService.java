package com.dnd.auth;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends plain-text emails over SMTP using {@link SmtpConfig}.
 *
 * <p>Throws {@link EmailNotConfiguredException} whenever credentials haven't
 * been filled in yet ({@code enabled=false} or blank username/password), so
 * callers (see {@link AuthService}) can fall back to displaying the
 * verification/reset code directly in the UI instead of failing the whole
 * registration/recovery flow.</p>
 */
public class EmailService {
    private final SmtpConfig config;

    public EmailService() {
        this(SmtpConfig.loadOrCreateTemplate());
    }

    public EmailService(SmtpConfig config) {
        this.config = config;
    }

    public boolean isConfigured() {
        return config.enabled
            && config.username != null && !config.username.isBlank()
            && config.password != null && !config.password.isBlank();
    }

    public void send(String to, String subject, String body) {
        if (!isConfigured()) {
            throw new EmailNotConfiguredException(
                "SMTP is not configured yet. Fill in real credentials at " + SmtpConfig.configPath()
                    + " and set \"enabled\": true to send real emails.");
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(config.useTls));
        props.put("mail.smtp.host", config.host);
        props.put("mail.smtp.port", String.valueOf(config.port));

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username, config.password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            String from = (config.fromAddress == null || config.fromAddress.isBlank())
                ? config.username : config.fromAddress;
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }

    /** Thrown when {@link #send} is called but SMTP credentials aren't set up. */
    public static class EmailNotConfiguredException extends RuntimeException {
        public EmailNotConfiguredException(String message) {
            super(message);
        }
    }
}
