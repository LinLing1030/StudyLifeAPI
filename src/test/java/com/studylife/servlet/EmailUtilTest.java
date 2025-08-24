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

        System.setProperty("SMTP_HOST", "localhost");
        System.setProperty("SMTP_PORT", "3025");
        System.setProperty("SMTP_STARTTLS", "false");
        System.setProperty("SMTP_SSL", "false");
        System.setProperty("SMTP_USER", "noreply@test.local");
        System.setProperty("SMTP_PASS", "dummy");
        System.setProperty("SMTP_FROM", "noreply@test.local");

        System.setProperty("MAIL_DEBUG", "true");
    }

    @After
    public void stopSmtp() {
        if (smtp != null) smtp.stop();

        System.clearProperty("SMTP_HOST");
        System.clearProperty("SMTP_PORT");
        System.clearProperty("SMTP_STARTTLS");
        System.clearProperty("SMTP_SSL");
        System.clearProperty("SMTP_USER");
        System.clearProperty("SMTP_PASS");
        System.clearProperty("SMTP_FROM");
        System.clearProperty("MAIL_DEBUG");
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

        Object content = msgs[0].getContent();
        String body = (content instanceof String) ? (String) content : content.toString();
        assertTrue(body.contains("<b>Hi</b>"));
    }
}
