package com.studylife.servlet;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailUtil {

    private static final String KEY_MAIL_USERNAME = "MAIL_USERNAME";
    private static final String KEY_MAIL_PASSWORD = "MAIL_PASSWORD";
    private static final String KEY_MAIL_FROM     = "MAIL_FROM";
    private static final String KEY_SMTP_HOST     = "SMTP_HOST";
    private static final String KEY_SMTP_PORT     = "SMTP_PORT";
    private static final String KEY_STARTTLS      = "STARTTLS";
    private static final String KEY_SSL           = "SSL";

    private static final String DEF_STARTTLS = "true";
    private static final String DEF_SSL      = "false";

    /** 读取配置：优先环境变量，其次 -Dsystem property */
    private static String cfg(String key, String def, boolean required) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = System.getProperty(key);
        if (v == null || v.isBlank()) {
            if (required) throw new IllegalStateException("Missing mail config: " + key);
            return def;
        }
        return v.trim();
    }

    /** 懒加载单例，避免每次 new Session */
    private static class Holder {
        static final Session SESSION = buildSession();

        private static Session buildSession() {
            final String username = cfg(KEY_MAIL_USERNAME, cfg(KEY_MAIL_FROM, null, true), true);
            final String password = cfg(KEY_MAIL_PASSWORD, null, true);
            final String host     = cfg(KEY_SMTP_HOST, null, true);
            final String port     = cfg(KEY_SMTP_PORT, "587", false);
            final boolean startTLS= Boolean.parseBoolean(cfg(KEY_STARTTLS, DEF_STARTTLS, false));
            final boolean ssl     = Boolean.parseBoolean(cfg(KEY_SSL, DEF_SSL, false));

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", String.valueOf(startTLS));
            props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);

            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }
    }

    /** 对外提供 Session */
    public static Session getSession() {
        return Holder.SESSION;
    }

    /** 发送邮件 */
    public static void sendMail(String to, String subject, String body) throws MessagingException {
        Message message = new MimeMessage(getSession());
        message.setFrom(new InternetAddress(cfg(KEY_MAIL_FROM, null, true)));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}
