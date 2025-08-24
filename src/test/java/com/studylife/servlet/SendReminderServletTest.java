package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class SendReminderServletTest {

    @Test
    public void invalidJson_shouldReturn4xx_or5xx_withErrorBody() throws Exception {
        StubHttpServletRequest req = new StubHttpServletRequest("{oops");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String body = safeBody(resp);

        assertTrue("expected 4xx/5xx for invalid JSON, got " + status, status >= 400 && status < 600);
        assertTrue("body should mention error/invalid, body=" + body,
                containsAnyIgnoreCase(body, "error", "invalid", "malformed", "bad request", "server error", "msg_"));
    }

    @Test
    public void pastTime_shouldReturn4xx_withTimeInvalidHint() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello")
                .put("date", LocalDate.now().minusDays(1).toString())
                .put("time", "00:01");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = safeBody(resp);

        assertTrue("expected 4xx for past time, got " + status, status >= 400 && status < 500);
        assertTrue("body should hint time invalid, body=" + out,
                containsAnyIgnoreCase(out,
                        "selected time", "already", "past", "invalid time", "msg_invalid_time", "before now", "minimum lead"));
    }

    @Test
    public void futureTime_shouldSucceed_2xx() throws Exception {
        LocalDateTime dt = LocalDateTime.now().plusMinutes(15);
        String date = dt.toLocalDate().toString();
        String time = dt.format(DateTimeFormatter.ofPattern("HH:mm"));

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
