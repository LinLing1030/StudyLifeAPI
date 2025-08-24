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
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

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
    public void missingFields_shouldReturn400_andMentionMissing() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "")
                .put("message", "hi")
                .put("date", "")
                .put("time", "");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out, "missing required fields", "missing", "error"));
    }

    @Test
    public void invalidDateOrTimeFormat_shouldReturn400() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "bad time")
                .put("date", "2025-99-99")
                .put("time", "25:99");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out, "invalid date or time format", "invalid", "error"));
    }

    @Test
    public void pastTime_shouldReturn4xx_withTimeInvalidHint() throws Exception {
        ZonedDateTime zdt = ZonedDateTime.now(ZONE).minusDays(1).withSecond(0).withNano(0);
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello")
                .put("date", zdt.toLocalDate().toString())
                .put("time", zdt.toLocalTime().format(HH_MM));

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out, "selected time", "already", "past", "invalid time", "msg_invalid_time", "before now", "minimum lead"));
    }

    @Test
    public void futureTime_shouldSucceed_2xx() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime base = now.plusMinutes(1).withSecond(0).withNano(0);
        ZonedDateTime zdt = base.plusMinutes(10);

        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello future")
                .put("date", zdt.toLocalDate().toString())
                .put("time", zdt.toLocalTime().format(HH_MM));

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 200 && status < 300);
        assertNotNull(out);
        assertTrue(containsAnyIgnoreCase(out, "scheduled", "ok", "success", "created", "accepted", "msg_ok"));
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
