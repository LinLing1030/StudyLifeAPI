package com.studylife.servlet;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailUtil {

    private static String propOrEnv(String propKey, String envKey, String defVal) {
        String v = System.getProperty(propKey);
        if (v == null || v.isEmpty()) v = System.getenv(envKey);
        return (v == null || v.isEmpty()) ? defVal : v;
    }

    private static boolean boolPropOrEnv(String propKey, String envKey, boolean defVal) {
        String s = propOrEnv(propKey, envKey, null);
        if (s == null) return defVal;
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static void sendEmail(String toEmail, String subject, String messageText) throws MessagingException {
        String host     = propOrEnv("MAIL_SMTP_HOST", "SMTP_HOST", "smtp.gmail.com");
        String port     = propOrEnv("MAIL_SMTP_PORT", "SMTP_PORT", "587");
        boolean auth    = boolPropOrEnv("MAIL_SMTP_AUTH", "SMTP_AUTH", true);
        boolean starttls= boolPropOrEnv("MAIL_STARTTLS",   "SMTP_STARTTLS", true);
        boolean ssl     = boolPropOrEnv("MAIL_SSL",        "SMTP_SSL", false);
        boolean debug   = boolPropOrEnv("MAIL_DEBUG",      "SMTP_DEBUG", false);

        String username = propOrEnv("MAIL_USERNAME", "SMTP_USER", null);
        String password = propOrEnv("MAIL_PASSWORD", "SMTP_PASS", null);
        String from     = propOrEnv("MAIL_FROM",     "SMTP_FROM", username); 

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        Authenticator authenticator = null;
        if (auth) {
            if (isBlank(username) || isBlank(password)) {
                throw new MessagingException("SMTP_USER / SMTP_PASS not configured in environment.");
            }
            authenticator = new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        Session session = Session.getInstance(props, authenticator);
        session.setDebug(debug);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(isBlank(from) ? "noreply@localhost" : from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(messageText, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }
}
