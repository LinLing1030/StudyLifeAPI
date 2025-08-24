package com.studylife.servlet;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {

    private static String prop(String sysKey, String envKey, String defVal) {
        String v = System.getProperty(sysKey);
        if (v == null || v.trim().isEmpty()) v = System.getenv(envKey);
        return (v == null || v.trim().isEmpty()) ? defVal : v.trim();
    }
    private static boolean propBool(String sysKey, String envKey, boolean defVal) {
        String v = prop(sysKey, envKey, defVal ? "true" : "false");
        return "1".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    public static void sendEmail(String toEmail, String subject, String messageText) throws MessagingException {
        String host     = prop("MAIL_SMTP_HOST", "SMTP_HOST", "smtp.gmail.com");
        String port     = prop("MAIL_SMTP_PORT", "SMTP_PORT", "587");
        boolean auth    = propBool("MAIL_SMTP_AUTH", "SMTP_AUTH", true);
        boolean starttls= propBool("MAIL_STARTTLS",  "SMTP_STARTTLS", true);
        boolean ssl     = propBool("MAIL_SSL",       "SMTP_SSL", false);
        boolean debug   = propBool("MAIL_DEBUG",     "SMTP_DEBUG", false);

        String username = prop("MAIL_USERNAME", "SMTP_USER", "");
        String password = prop("MAIL_PASSWORD", "SMTP_PASS", "");
        String from     = prop("MAIL_FROM",     "SMTP_FROM",
                !isBlank(username) ? username : "noreply@example.com");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        boolean doAuth = auth && !isBlank(username) && !isBlank(password);
        props.put("mail.smtp.auth", String.valueOf(doAuth));

        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        if (ssl) props.put("mail.smtp.ssl.enable", "true");

        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        Authenticator authenticator = null;
        if (doAuth) {
            final String u = username, p = password;
            authenticator = new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(u, p);
                }
            };
        }

        Session session = Session.getInstance(props, authenticator);
        session.setDebug(debug);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(messageText, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }
}
