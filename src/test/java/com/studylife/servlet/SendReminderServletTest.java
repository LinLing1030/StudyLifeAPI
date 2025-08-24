package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class SendReminderServletTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    @Test
    public void invalidJson_shouldReturn4xx_withErrorBody() throws Exception {
        StubHttpServletRequest req = new StubHttpServletRequest("{oops");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String body = bodyOf(resp);
        assertTrue("expect 4xx for invalid json, got " + status, status >= 400 && status < 500);
        assertTrue(body.toLowerCase().contains("error"));
    }

    @Test
    public void missingFields_shouldReturn400() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello");
        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, resp.getStatus());
        assertTrue(bodyOf(resp).toLowerCase().contains("missing"));
    }


    @Test
    public void invalidDateOrTime_shouldReturn400() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello")
                .put("date", "2025/08/24")  
                .put("time", "25:99");     
        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, resp.getStatus());
        String out = bodyOf(resp).toLowerCase();
        assertTrue(out.contains("invalid"));
        assertTrue(out.contains("format"));
    }


    @Test
    public void pastTime_shouldReturn4xx_withTimeInvalidHint() throws Exception {
        ZonedDateTime zdt = ZonedDateTime.now(ZONE).minusDays(1);
        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello")
                .put("date", zdt.toLocalDate().toString())
                .put("time", zdt.toLocalTime().withSecond(0).withNano(0).format(HM));

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = bodyOf(resp).toLowerCase();
        assertTrue("expected 4xx for past time, got " + status, status >= 400 && status < 500);
        assertTrue(containsAny(out, "selected time", "past", "lead", "invalid time", "before"));
    }


    @Test
    public void futureTime_shouldSucceed_2xx() throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime nextMinute = now.plusMinutes(1).withSecond(0).withNano(0);
        ZonedDateTime target = nextMinute.plusMinutes(15);

        JSONObject body = new JSONObject()
                .put("email", "u@test.local")
                .put("message", "hello future")
                .put("date", target.toLocalDate().toString())
                .put("time", target.toLocalTime().format(HM));

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doPost(req, resp);

        int status = resp.getStatus();
        String out = bodyOf(resp).toLowerCase();
        assertTrue("expected 2xx, got " + status, status >= 200 && status < 300);
        assertTrue(containsAny(out, "scheduled", "ok", "success", "created", "accepted"));
    }

    @Test
    public void options_shouldSetCorsAnd200() throws Exception {
        StubHttpServletRequest req = new StubHttpServletRequest("");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new SendReminderServlet().doOptions(req, resp);

        assertEquals(200, resp.getStatus());
        assertEquals("*", resp.getHeader("Access-Control-Allow-Origin"));
        assertTrue(resp.getHeader("Access-Control-Allow-Methods").contains("POST"));
        assertTrue(resp.getHeader("Access-Control-Allow-Headers").contains("Content-Type"));
    }


    private static String bodyOf(StubHttpServletResponse resp) {
        String b = resp.getBody();
        return b == null ? "" : b;
    }

    private static boolean containsAny(String hay, String... needles) {
        String h = hay == null ? "" : hay.toLowerCase();
        for (String n : needles) {
            if (n != null && h.contains(n.toLowerCase())) return true;
        }
        return false;
    }
}
