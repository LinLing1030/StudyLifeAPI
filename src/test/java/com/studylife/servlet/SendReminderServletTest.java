package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedStatic;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;

public class SendReminderServletTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

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
                .put("message", "m")
                .put("date", "")
                .put("time", "");
        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();
        new SendReminderServlet().doPost(req, resp);
        int status = resp.getStatus();
        String out = safeBody(resp);
        assertTrue(status >= 400 && status < 500);
        assertTrue(containsAnyIgnoreCase(out, "missing", "required", "error"));
    }

    @Test
    public void pastTime_shouldReturn4xx_withTimeInvalidHint() throws Exception {
        ZonedDateTime zdt = ZonedDateTime.now(ZONE).minusDays(1);
        String date = zdt.toLocalDate().toString();
        String time = zdt.toLocalTime().withSecond(0).withNano(0).format(HHMM);
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
        assertTrue(containsAnyIgnoreCase(out, "selected time", "already", "past", "invalid time", "msg_invalid_time", "before now", "minimum lead"));
    }

    @Test
    public void futureTime_shouldSucceed_2xx_withMinLead0() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime zdt = now.plusMinutes(2).withSecond(0).withNano(0);
        String date = zdt.toLocalDate().toString();
        String time = zdt.toLocalTime().format(HHMM);
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello future")
                .put("date", date)
                .put("time", time);
        try (MockedStatic<System> ms = mockStatic(System.class)) {
            ms.when(() -> System.getenv("REMINDER_MIN_LEAD_MINUTES")).thenReturn("0");
            StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
            StubHttpServletResponse resp = new StubHttpServletResponse();
            new SendReminderServlet().doPost(req, resp);
            int status = resp.getStatus();
            String out = safeBody(resp);
            assertTrue(status >= 200 && status < 300);
            assertNotNull(out);
            assertTrue(containsAnyIgnoreCase(out, "scheduled", "ok", "success", "created", "accepted", "msg_ok"));
        }
    }

    @Test
    public void invalidMinLeadEnv_shouldReturn4xx_dueToMinimumLeadEnforced() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime zdt = now.plusMinutes(1).withSecond(0).withNano(0);
        String date = zdt.toLocalDate().toString();
        String time = zdt.toLocalTime().format(HHMM);
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hi")
                .put("date", date)
                .put("time", time);
        try (MockedStatic<System> ms = mockStatic(System.class)) {
            ms.when(() -> System.getenv("REMINDER_MIN_LEAD_MINUTES")).thenReturn("abc");
            StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
            StubHttpServletResponse resp = new StubHttpServletResponse();
            new SendReminderServlet().doPost(req, resp);
            int status = resp.getStatus();
            String out = safeBody(resp);
            assertTrue(status >= 400 && status < 500);
            assertTrue(containsAnyIgnoreCase(out, "minimum", "lead", "minutes", "before now"));
        }
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
