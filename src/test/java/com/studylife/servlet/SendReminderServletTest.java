package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class SendReminderServletTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Dublin");

    @Test
    public void invalidJson_shouldReturn4xx_or5xx_withErrorBody() throws Exception {
        StubHttpServletRequest req = new StubHttpServletRequest("{oops");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String body = safeBody(resp);
        assertTrue(status >= 400 && status < 600);
        assertTrue(containsAnyIgnoreCase(body, "error", "invalid", "malformed", "bad request", "server error", "msg_"));
    }

    @Test
    public void missingFields_shouldReturn400() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "")
                .put("message", "")
                .put("date", "")
                .put("time", "");
        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out, "missing required fields", "error"));
    }

    @Test
    public void invalidDateFormat_shouldReturn400_withFormatMessage() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hi")
                .put("date", "2025/01/01")
                .put("time", "99:99");
        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out, "invalid date or time format", "error"));
    }

    @Test
    public void pastTime_shouldReturn4xx_withTimeInvalidHint() throws Exception {
        ZonedDateTime zdt = ZonedDateTime.now(ZONE).minusDays(1);
        String date = zdt.toLocalDate().toString();
        String time = zdt.toLocalTime().withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello")
                .put("date", date)
                .put("time", time);

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);

        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out,
                "selected time", "already", "past", "invalid time", "msg_invalid_time", "before now", "minimum lead"));
    }

    @Test
    public void futureTime_shouldSucceed_2xx() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime nextMinute = now.plusMinutes(1).withSecond(0).withNano(0);
        ZonedDateTime zdt = nextMinute.plusMinutes(15);

        String date = zdt.toLocalDate().toString();
        String time = zdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello future")
                .put("date", date)
                .put("time", time);

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);

        assertTrue(status >= 200 && status < 300);
        assertNotNull(out);
        assertTrue(containsAnyIgnoreCase(out, "scheduled", "ok", "success", "created", "accepted", "msg_ok"));
    }

    @Test
    public void doOptions_shouldSetCors_and200() throws Exception {
        StubHttpServletRequest req = new StubHttpServletRequest("");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doOptions(req, resp);

        assertEquals(200, resp.getStatus());
        String allowOrigin = resp.getHeader("Access-Control-Allow-Origin");
        String allowMethods = resp.getHeader("Access-Control-Allow-Methods");
        String allowHeaders = resp.getHeader("Access-Control-Allow-Headers");
        assertEquals("*", allowOrigin);
        assertTrue(containsAnyIgnoreCase(allowMethods, "post", "get", "options"));
        assertTrue(containsAnyIgnoreCase(allowHeaders, "content-type", "authorization"));
    }

    private static String safeBody(StubHttpServletResponse resp) {
        String b = resp.getBody();
        return b == null ? "" : b;
    }

    private static boolean containsAnyIgnoreCase(String text, String... needles) {
        String t = text == null ? "" : text.toLowerCase();
        for (String n : needles) {
            if (n != null && t.contains(n.toLowerCase())) return true;
        }
        return false;
    }
}
