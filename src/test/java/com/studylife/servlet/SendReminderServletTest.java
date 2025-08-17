package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class SendReminderServletTest {

    /**
     * 坏 JSON：只要出现 4xx/5xx 或正文里含 error/invalid 就认为进入了错误分支
     */
    @Test
    public void invalidJson_4xx_or_5xx() throws Exception {
        // 传入非法 JSON 字符串
        StubHttpServletRequest req = new StubHttpServletRequest("{oops");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String body = resp.getBody();

        boolean hasErrWord = body != null &&
                (body.toLowerCase().contains("error") || body.toLowerCase().contains("invalid"));

        assertTrue(
            "expect 4xx/5xx or error text, status=" + status + ", body=" + body,
            (status >= 400 && status < 600) || hasErrWord
        );
    }

    /**
     * 过去时间：应返回 4xx，并包含“Selected time”/“already”/“MSG_INVALID_TIME”或错误字样
     */
    @Test
    public void pastTime_shouldFail_400_like() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello")
                .put("date", LocalDate.now().minusDays(1).toString())
                .put("time", "00:01");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        assertTrue("expect >=400, got " + status, status >= 400);

        String out = resp.getBody();
        assertNotNull(out);
        assertTrue(
            out.contains("Selected time") ||
            out.contains("already") ||
            out.contains("MSG_INVALID_TIME") ||
            out.toLowerCase().contains("error")
        );
    }
}
