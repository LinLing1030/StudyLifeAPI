package com.studylife.servlet;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public final class EmailUtil {
	  private static String p(String k, String d){ return System.getProperty(k, d); }

	  public static void sendEmail(String to, String subject, String text) throws Exception {
	    String username = p("MAIL_USERNAME", "noreply@test.local");
	    String password = p("MAIL_PASSWORD", "");
	    String host     = p("MAIL_SMTP_HOST", "localhost");
	    String port     = p("MAIL_SMTP_PORT", "3025");
	    String from     = p("MAIL_FROM", username);

	    Properties props = new Properties();
	    props.put("mail.smtp.host", host);
	    props.put("mail.smtp.port", port);
	    props.put("mail.smtp.auth",            p("MAIL_SMTP_AUTH", "false"));
	    props.put("mail.smtp.starttls.enable", p("MAIL_STARTTLS",  "false"));
	    props.put("mail.smtp.ssl.enable",      p("MAIL_SSL",       "false"));

	    Session session = Session.getInstance(props,
	        "true".equals(props.getProperty("mail.smtp.auth"))
	          ? new Authenticator() {
	              @Override protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(username, password);
	              }
	            }
	          : null);

	    Message msg = new MimeMessage(session);
	    msg.setFrom(new InternetAddress(from));
	    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
	    msg.setSubject(subject);
	    msg.setText(text);
	    Transport.send(msg);
	  }

	  public static void sendHtmlEmail(String to, String subject, String html) throws Exception {
	    String username = p("MAIL_USERNAME", "noreply@test.local");
	    String password = p("MAIL_PASSWORD", "");
	    String host     = p("MAIL_SMTP_HOST", "localhost");
	    String port     = p("MAIL_SMTP_PORT", "3025");
	    String from     = p("MAIL_FROM", username);

	    Properties props = new Properties();
	    props.put("mail.smtp.host", host);
	    props.put("mail.smtp.port", port);
	    props.put("mail.smtp.auth",            p("MAIL_SMTP_AUTH", "false"));
	    props.put("mail.smtp.starttls.enable", p("MAIL_STARTTLS",  "false"));
	    props.put("mail.smtp.ssl.enable",      p("MAIL_SSL",       "false"));

	    Session session = Session.getInstance(props,
	        "true".equals(props.getProperty("mail.smtp.auth"))
	          ? new Authenticator() {
	              @Override protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(username, password);
	              }
	            }
	          : null);

	    MimeMessage msg = new MimeMessage(session);
	    msg.setFrom(new InternetAddress(from));
	    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
	    msg.setSubject(subject);
	    msg.setContent(html, "text/html; charset=UTF-8"); // ← HTML
	    Transport.send(msg);
	  }
	}