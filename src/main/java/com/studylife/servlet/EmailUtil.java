//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.studylife.servlet;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {
    public EmailUtil() {
    }

    public static void sendEmail(String toEmail, String subject, String messageText) throws MessagingException {
        String fromEmail = "llguo10300@gmail.com";
        String password = "ocfl oicq uity xiyu";
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("llguo10300@gmail.com", "ocfl oicq uity xiyu");
            }
        });
        Message message = new MimeMessage(session);
        ((Message)message).setFrom(new InternetAddress("llguo10300@gmail.com"));
        ((Message)message).setRecipients(RecipientType.TO, InternetAddress.parse(toEmail));
        ((Message)message).setSubject(subject);
        ((Message)message).setText(messageText);
        Transport.send(message);
    }
}
