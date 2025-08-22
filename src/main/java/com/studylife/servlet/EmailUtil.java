package com.studylife.servlet;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {

    
    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isEmpty()) ? def : v;
    }

    public static void sendEmail(String toEmail, String subject, String messageText) throws MessagingException {
        final String fromEmail = env("MAIL_FROM", "llguo10300@gmail.com");
        final String username  = env("MAIL_USER", "llguo10300@gmail.com");
        final String password  = env("MAIL_PASS", "ocfloicquityxiyu"); // ← 没有空格！

        Properties props = new Properties();
        props.put("mail.smtp.host", env("MAIL_HOST", "smtp.gmail.com"));
        props.put("mail.smtp.port", env("MAIL_PORT", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "10000");
      
        // Session session = Session.getInstance(props); session.setDebug(true);
        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(messageText, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }
}
