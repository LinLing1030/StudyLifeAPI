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
        String host      = prop("MAIL_SMTP_HOST", "SMTP_HOST", "smtp.gmail.com");
        String port      = prop("MAIL_SMTP_PORT", "SMTP_PORT", "587");
        boolean starttls = propBool("MAIL_STARTTLS",  "SMTP_STARTTLS", true);
        boolean ssl      = propBool("MAIL_SSL",       "SMTP_SSL", false); 
        boolean debug    = propBool("MAIL_DEBUG",     "SMTP_DEBUG", false);

        String username  = prop("MAIL_USERNAME", "SMTP_USER", "");
        String password  = prop("MAIL_PASSWORD", "SMTP_PASS", "");
        String fromCfg   = prop("MAIL_FROM",     "SMTP_FROM",
                !isBlank(username) ? username : "noreply@example.com");


        boolean doAuth = !isBlank(username) && !isBlank(password);
        if (!doAuth) {
            throw new MessagingException(
                "SMTP auth required: set MAIL_USERNAME/SMTP_USER and MAIL_PASSWORD/SMTP_PASS (use Gmail App Password if using Gmail).");
        }

        String from;
        if ("smtp.gmail.com".equalsIgnoreCase(host)) {
            from = username;
        } else {
            from = isBlank(fromCfg) ? username : fromCfg;
        }


        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");


        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        if (starttls) {
            props.put("mail.smtp.starttls.required", "true");
        }

        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        }

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");


        final String u = username, p = password;
        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(u, p);
            }
        });
        session.setDebug(debug);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(messageText, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }
}
