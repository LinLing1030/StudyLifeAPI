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
    private static final String LEAD_KEY = "REMINDER_MIN_LEAD_MINUTES";

    @Test
    public void invalidJson_shouldReturn4xx_or5xx_withErrorBody() throws Exception {
        try {
            StubHttpServletRequest req = new StubHttpServletRequest("{oops");
            StubHttpServletResponse resp = new StubHttpServletResponse();

            new SendReminderServlet().doPost(req, resp);

            int status = resp.getStatus();
            String body = safeBody(resp);

            assertTrue("expected 4xx/5xx for invalid JSON, got " + status, status >= 400 && status < 600);
            assertTrue("body should mention error/invalid, body=" + body,
                    containsAnyIgnoreCase(body, "error", "invalid", "malformed", "bad request", "server error", "msg_"));
        } finally {
            System.clearProperty(LEAD_KEY);
        }
    }

    @Test
    public void pastTime_shouldReturn4xx_withTimeInvalidHint() throws Exception {
        try {
            System.clearProperty(LEAD_KEY);

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

            assertTrue("expected 4xx for past time, got " + status, status >= 400 && status < 500);
            assertTrue("body should hint time invalid, body=" + out,
                    containsAnyIgnoreCase(out,
                            "selected time", "already", "past", "invalid time", "msg_invalid_time", "before now", "minimum lead"));
        } finally {
            System.clearProperty(LEAD_KEY);
        }
    }

    @Test
    public void futureTime_shouldSucceed_2xx_withMinLead0() throws Exception {
        try {
            System.setProperty(LEAD_KEY, "0");

            ZonedDateTime now = ZonedDateTime.now(ZONE);
            ZonedDateTime zdt = now.plusMinutes(2).withSecond(0).withNano(0);

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

            assertTrue("expected 2xx for future time, got " + status, status >= 200 && status < 300);
            assertNotNull(out);
            assertTrue("body should acknowledge scheduling, body=" + out,
                    containsAnyIgnoreCase(out, "scheduled", "ok", "success", "created", "accepted", "msg_ok"));
        } finally {
            System.clearProperty(LEAD_KEY);
        }
    }

    @Test
    public void invalidMinLeadEnv_shouldReturn4xx_dueToMinimumLeadEnforced() throws Exception {
        try {
            System.setProperty(LEAD_KEY, "30");

            ZonedDateTime now = ZonedDateTime.now(ZONE);
            ZonedDateTime zdt = now.plusMinutes(5).withSecond(0).withNano(0);

            String date = zdt.toLocalDate().toString();
            String time = zdt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

            JSONObject body = new JSONObject()
                    .put("email", "u@test.local")
                    .put("message", "too soon")
                    .put("date", date)
                    .put("time", time);

            StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
            StubHttpServletResponse resp = new StubHttpServletResponse();

            new SendReminderServlet().doPost(req, resp);

            int status = resp.getStatus();
            String out = safeBody(resp);

            assertTrue("expect 4xx when below minimum lead, got " + status, status >= 400 && status < 500);
            assertTrue("body should mention minimum lead, body=" + out,
                    containsAnyIgnoreCase(out, "minimum lead", "before minimum", "selected time"));
        } finally {
            System.clearProperty(LEAD_KEY);
        }
    }

    // ---------- helpers ----------
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
