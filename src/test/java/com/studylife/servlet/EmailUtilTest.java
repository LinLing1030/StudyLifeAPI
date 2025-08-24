package com.studylife.servlet;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EmailUtilTest {

    private GreenMail smtp;

    @Before
    public void startSmtp() {
        smtp = new GreenMail(ServerSetupTest.SMTP);
        smtp.start();

        smtp.setUser("noreply@test.local", "noreply@test.local", "dummy");


        System.setProperty("MAIL_SMTP_HOST", "localhost");
        System.setProperty("MAIL_SMTP_PORT", "3025");
        System.setProperty("MAIL_STARTTLS",  "false");
        System.setProperty("MAIL_SSL",       "false");
        System.setProperty("MAIL_USERNAME",  "noreply@test.local");
        System.setProperty("MAIL_PASSWORD",  "dummy");
        System.setProperty("MAIL_FROM",      "noreply@test.local");
        System.setProperty("MAIL_DEBUG",     "false");

        System.setProperty("SMTP_HOST", "localhost");
        System.setProperty("SMTP_PORT", "3025");
        System.setProperty("SMTP_STARTTLS", "false");
        System.setProperty("SMTP_SSL", "false");
        System.setProperty("SMTP_USER", "noreply@test.local");
        System.setProperty("SMTP_PASS", "dummy");
        System.setProperty("SMTP_FROM", "noreply@test.local");
    }

    @After
    public void stopSmtp() {
        if (smtp != null) smtp.stop();

        String[] keys = {
            "MAIL_SMTP_HOST","MAIL_SMTP_PORT","MAIL_STARTTLS","MAIL_SSL",
            "MAIL_USERNAME","MAIL_PASSWORD","MAIL_FROM","MAIL_DEBUG",
            "SMTP_HOST","SMTP_PORT","SMTP_STARTTLS","SMTP_SSL",
            "SMTP_USER","SMTP_PASS","SMTP_FROM"
        };
        for (String k: keys) System.clearProperty(k);
    }

    @Test
    public void sendPlainEmail_ok() throws Exception {
        EmailUtil.sendEmail("user@test.local", "Unit Test", "Hello from unit test.");
        smtp.waitForIncomingEmail(1);
        MimeMessage[] msgs = smtp.getReceivedMessages();
        assertEquals(1, msgs.length);
        assertEquals("Unit Test", msgs[0].getSubject());
    }

    @Test
    public void sendHtmlEmail_ok() throws Exception {
        EmailUtil.sendEmail("user@test.local", "HTML", "<b>Hi</b>");
        smtp.waitForIncomingEmail(1);
        MimeMessage[] msgs = smtp.getReceivedMessages();
        assertEquals(1, msgs.length);
        assertEquals("HTML", msgs[0].getSubject());
        String raw = new String(smtp.getReceivedMessages()[0].getRawInputStream().readAllBytes());
        assertTrue(raw.contains("<b>Hi</b>"));
    }
}
