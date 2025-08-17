package com.studylife.servlet;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * EmailUtil 的集成测试：使用本地 GreenMail SMTP (localhost:3025)。
 * 依赖：
 *   - com.icegreen:greenmail:1.6.10 (test)
 *   - com.sun.mail:javax.mail:1.6.2
 */
public class EmailUtilTest {

    private GreenMail smtp;

    @Before
    public void startSmtp() {
        // 启动一个本地 SMTP (端口 3025)，不会访问外网
        smtp = new GreenMail(ServerSetupTest.SMTP);
        smtp.start();

        // 配置 EmailUtil 读取到的系统属性 —— 禁用鉴权/加密，全部走本地
        System.setProperty("MAIL_USERNAME",  "noreply@test.local");
        System.setProperty("MAIL_PASSWORD",  "dummy");
        System.setProperty("MAIL_FROM",      "noreply@test.local");

        System.setProperty("MAIL_SMTP_HOST", "localhost");
        System.setProperty("MAIL_SMTP_PORT", "3025");
        System.setProperty("MAIL_SMTP_AUTH", "false");
        System.setProperty("MAIL_STARTTLS",  "false");
        System.setProperty("MAIL_SSL",       "false");
    }

    @After
    public void stopSmtp() {
        if (smtp != null) {
            smtp.stop();
        }
        // 清理系统属性，避免影响其他用例
        System.clearProperty("MAIL_USERNAME");
        System.clearProperty("MAIL_PASSWORD");
        System.clearProperty("MAIL_FROM");
        System.clearProperty("MAIL_SMTP_HOST");
        System.clearProperty("MAIL_SMTP_PORT");
        System.clearProperty("MAIL_SMTP_AUTH");
        System.clearProperty("MAIL_STARTTLS");
        System.clearProperty("MAIL_SSL");
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
        EmailUtil.sendHtmlEmail("user@test.local", "HTML", "<b>Hi</b>");

        smtp.waitForIncomingEmail(1);
        MimeMessage[] msgs = smtp.getReceivedMessages();
        assertEquals(1, msgs.length);
        assertEquals("HTML", msgs[0].getSubject());
        String content = (String) msgs[0].getContent();
        assertTrue(content.contains("<b>Hi</b>"));
    }
}
