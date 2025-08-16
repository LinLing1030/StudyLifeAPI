package com.studylife.servlet;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class EmailUtil {

    // ---- Keys (避免重复字面量) ----
    private static final String KEY_MAIL_USERNAME = "MAIL_USERNAME";
    private static final String KEY_MAIL_FROM     = "MAIL_FROM";
    private static final String KEY_MAIL_PASSWORD = "MAIL_PASSWORD";
    private static final String KEY_SMTP_HOST     = "MAIL_SMTP_HOST";
    private static final String KEY_SMTP_PORT     = "MAIL_SMTP_PORT";
    private static final String KEY_STARTTLS      = "MAIL_STARTTLS";
    private static final String KEY_SSL           = "MAIL_SSL";

    // ---- Defaults ----
    private static final String DEF_HOST      = "smtp.gmail.com";
    private static final String DEF_PORT      = "587";   // 465 用 SSL
    private static final String DEF_STARTTLS  = "true";  // 对 587
    private static final String DEF_SSL       = "false"; // 对 465

    private EmailUtil() {} // 禁止实例化

    /** 环境变量 > -Dsystem property；required=true 未配置则抛错 */
    private static String cfg(String key, String def, boolean required) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = System.getProperty(key);
        if (v == null || v.isBlank()) {
            if (required) throw new IllegalStateException("Missing mail config: " + key);
            return def;
        }
        return v.trim();
    }

    /** 懒加载 Session */
    private static class Holder {
        static final Session SESSION = buildSession();
    }

    private static Session buildSession() {
        final String username = cfg(KEY_MAIL_USERNAME, cfg(KEY_MAIL_FROM, null, true), true);
        final String password = cfg(KEY_MAIL_PASSWORD, null, true);
        final String host     = cfg(KEY_SMTP_HOST, DEF_HOST, false);
        final String port     = cfg(KEY_SMTP_PORT, DEF_PORT, false);
        final boolean startTLS= Boolean.parseBoolean(cfg(KEY_STARTTLS, DEF_STARTTLS, false));
        final boolean ssl     = Boolean.parseBoolean(cfg(KEY_SSL, DEF_SSL, false));

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(startTLS));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "20000");
        props.put("mail.smtp.writetimeout", "20000");

        return Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /** 纯文本 */
    public static void sendMail(String toEmail, String subject, String messageText) throws MessagingException {
        final Session session = Holder.SESSION;
        final String from = cfg(KEY_MAIL_FROM, cfg(KEY_MAIL_USERNAME, null, true), true);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setText(messageText, StandardCharsets.UTF_8.name());
        Transport.send(msg);
    }

    /** HTML（可选） */
    public static void sendHtmlMail(String toEmail, String subject, String html) throws MessagingException {
        final Session session = Holder.SESSION;
        final String from = cfg(KEY_MAIL_FROM, cfg(KEY_MAIL_USERNAME, null, true), true);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setContent(html, "text/html; charset=UTF-8");
        Transport.send(msg);
    }
}
