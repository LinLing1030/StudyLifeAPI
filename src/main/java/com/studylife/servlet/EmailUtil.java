package com.studylife.servlet;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class EmailUtil {


    private static String propOrEnv(String propKey, String envKey, String def) {
        String v = System.getProperty(propKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private static boolean boolPropOrEnv(String propKey, String envKey, boolean def) {
        String v = propOrEnv(propKey, envKey, null);
        if (v == null) return def;
        return "1".equalsIgnoreCase(v)
                || "true".equalsIgnoreCase(v)
                || "yes".equalsIgnoreCase(v);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean mailDebug() {
        return boolPropOrEnv("MAIL_DEBUG", "MAIL_DEBUG", false);
    }

    public static void sendEmail(String toEmail, String subject, String messageText) throws MessagingException {
        String from     = propOrEnv("MAIL_FROM",       "SMTP_FROM", null);
        String host     = propOrEnv("MAIL_SMTP_HOST",  "SMTP_HOST", "smtp.gmail.com");
        String port     = propOrEnv("MAIL_SMTP_PORT",  "SMTP_PORT", "587");
        boolean auth    = boolPropOrEnv("MAIL_SMTP_AUTH",  "SMTP_AUTH", true);  
        boolean starttls= boolPropOrEnv("MAIL_STARTTLS",   "SMTP_STARTTLS", true);
        boolean ssl     = boolPropOrEnv("MAIL_SSL",        "SMTP_SSL", false);

        String username = propOrEnv("MAIL_USERNAME",  "SMTP_USER", null);
        String password = propOrEnv("MAIL_PASSWORD",  "SMTP_PASS", null);

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));

        if (starttls) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.ssl.trust", host);
        }

        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
        }

        Session session;
        if (auth) {
            if (isBlank(username) || isBlank(password)) {
                throw new MessagingException("SMTP_USER / SMTP_PASS not configured in environment.");
            }
            final String u = username;
            final String p = password;
            session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(u, p);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        session.setDebug(mailDebug());

        MimeMessage message = new MimeMessage(session);

        if (!isBlank(from)) {
            message.setFrom(new InternetAddress(from));
        }

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(messageText, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }
}
