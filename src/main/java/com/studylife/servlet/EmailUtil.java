package com.studylife.servlet;

import javax.mail.*;
import javax.mail.internet.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class EmailUtil {

    /** 读取配置：优先环境变量，其次 -Dsystem property；required=true 时未配置会抛错 */
    private static String cfg(String key, String def, boolean required) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = System.getProperty(key);
        if ((v == null || v.isBlank())) {
            if (required) throw new IllegalStateException("Missing mail config: " + key);
            return def;
        }
        return v.trim();
    }

    /** 懒加载复用会话，避免每次都 new Session */
    private static class Holder {
        static final Session SESSION = buildSession();
    }

    private static Session buildSession() {
        // 这些键都可用环境变量或 -D 传入
        // 常用键：
        // MAIL_FROM / MAIL_USERNAME：发件人账号（Gmail/SES SMTP 用户名）
        // MAIL_PASSWORD：密码或 SMTP 密钥
        // MAIL_SMTP_HOST：SMTP 主机（Gmail: smtp.gmail.com；SES: email-smtp.<region>.amazonaws.com）
        // MAIL_SMTP_PORT：587 或 465
        // MAIL_STARTTLS：true/false（587 通常用 true）
        // MAIL_SSL：true/false（465 通常用 true）
        final String username = cfg("MAIL_USERNAME", cfg("MAIL_FROM", null, true), true);
        final String password = cfg("MAIL_PASSWORD", null, true);
        final String host     = cfg("MAIL_SMTP_HOST", "smtp.gmail.com", false); // 若用 SES，请设置为 email-smtp.<region>.amazonaws.com
        final String port     = cfg("MAIL_SMTP_PORT", "587", false);
        final boolean startTLS= Boolean.parseBoolean(cfg("MAIL_STARTTLS", "true", false));
        final boolean ssl     = Boolean.parseBoolean(cfg("MAIL_SSL", "false", false));

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(startTLS));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        // 连接超时设置，避免云上网络偶发卡死
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "20000");
        props.put("mail.smtp.writetimeout", "20000");

        return Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /** 发送纯文本邮件 */
    public static void sendEmail(String toEmail, String subject, String messageText) throws MessagingException {
        final Session session = Holder.SESSION;

        String from = cfg("MAIL_FROM", cfg("MAIL_USERNAME", null, true), true);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setText(messageText, StandardCharsets.UTF_8.name());
        Transport.send(msg);
    }

    /** 发送 HTML 邮件（需要时可用） */
    public static void sendHtmlEmail(String toEmail, String subject, String html) throws MessagingException {
        final Session session = Holder.SESSION;

        String from = cfg("MAIL_FROM", cfg("MAIL_USERNAME", null, true), true);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());
        msg.setContent(html, "text/html; charset=UTF-8");
        Transport.send(msg);
    }
}
