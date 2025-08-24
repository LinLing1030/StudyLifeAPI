package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class SendReminderServletTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    static class DirectScheduler extends ScheduledThreadPoolExecutor {
        DirectScheduler() { super(1); }
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            command.run();
            return super.schedule(() -> {}, 0, TimeUnit.MILLISECONDS);
        }
    }

    static class TestableServlet extends SendReminderServlet {
        private final Map<String,String> env;
        TestableServlet(ScheduledExecutorService sch, EmailSender sender, Map<String,String> env) {
            super(sch, sender);
            this.env = env;
        }
        @Override
        protected String getEnv(String key) {
            return env == null ? super.getEnv(key) : env.get(key);
        }
    }

    @Test
    public void doOptions_should200_andCors() throws Exception {
        SendReminderServlet s = new SendReminderServlet(new DirectScheduler(), (a,b,c)->{});
        StubHttpServletRequest req = new StubHttpServletRequest("");
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doOptions(req, resp);
        assertEquals(200, resp.getStatus());
        assertNotNull(resp.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void invalidJson_should400() throws Exception {
        SendReminderServlet s = new SendReminderServlet(new DirectScheduler(), (a,b,c)->{});
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doPost(new StubHttpServletRequest("{oops"), resp);
        assertTrue(resp.getStatus() >= 400 && resp.getStatus() < 500);
        assertTrue(containsAny(resp.getBody(), "invalid", "error", "bad"));
    }

    @Test
    public void missingFields_should400() throws Exception {
        SendReminderServlet s = new SendReminderServlet(new DirectScheduler(), (a,b,c)->{});
        JSONObject body = new JSONObject().put("email","a@b.com");
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doPost(new StubHttpServletRequest(body.toString()), resp);
        assertTrue(resp.getStatus() >= 400 && resp.getStatus() < 500);
        assertTrue(containsAny(resp.getBody(), "missing", "error"));
    }

    @Test
    public void invalidDateFormat_should400() throws Exception {
        SendReminderServlet s = new SendReminderServlet(new DirectScheduler(), (a,b,c)->{});
        JSONObject body = new JSONObject()
                .put("email","u@test.local")
                .put("message","x")
                .put("date","2025-13-40")
                .put("time","99:99");
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doPost(new StubHttpServletRequest(body.toString()), resp);
        assertTrue(resp.getStatus() >= 400 && resp.getStatus() < 500);
        assertTrue(containsAny(resp.getBody(), "invalid", "error", "format"));
    }

    @Test
    public void minLead_invalidNumber_shouldFallback5() throws Exception {
        SendReminderServlet s = new TestableServlet(new DirectScheduler(), (a,b,c)->{}, Map.of("REMINDER_MIN_LEAD_MINUTES","abc"));
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime target = now.plusMinutes(1).withSecond(0).withNano(0);
        JSONObject body = new JSONObject()
                .put("email","u@test.local")
                .put("message","m")
                .put("date", target.toLocalDate().toString())
                .put("time", target.toLocalTime().format(HM));
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doPost(new StubHttpServletRequest(body.toString()), resp);
        assertTrue(resp.getStatus() >= 400 && resp.getStatus() < 500);
        assertTrue(containsAny(resp.getBody(), "lead", "past", "error"));
    }

    @Test
    public void futureTime_shouldSucceed_2xx_withMinLead0() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        SendReminderServlet.EmailSender ok = (to,sub,txt) -> called.set(true);
        SendReminderServlet s = new TestableServlet(new DirectScheduler(), ok, Map.of("REMINDER_MIN_LEAD_MINUTES","0"));
        ZonedDateTime base = ZonedDateTime.now(ZONE).plusMinutes(1).withSecond(0).withNano(0);
        ZonedDateTime tgt = base.plusMinutes(1);
        JSONObject body = new JSONObject()
                .put("email","u@test.local")
                .put("message","hello future")
                .put("date", tgt.toLocalDate().toString())
                .put("time", tgt.toLocalTime().format(HM));
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doPost(new StubHttpServletRequest(body.toString()), resp);
        assertTrue(resp.getStatus() >= 200 && resp.getStatus() < 300);
        assertTrue(containsAny(resp.getBody(), "scheduled", "ok", "success"));
        assertTrue(called.get());
    }

    @Test
    public void futureTime_emailFailure_shouldStillReturn200_andCoverCatch() throws Exception {
        SendReminderServlet.EmailSender fail = (to,sub,txt) -> { throw new RuntimeException("boom"); };
        SendReminderServlet s = new TestableServlet(new DirectScheduler(), fail, Map.of("REMINDER_MIN_LEAD_MINUTES","0"));
        ZonedDateTime base = ZonedDateTime.now(ZONE).plusMinutes(1).withSecond(0).withNano(0);
        ZonedDateTime tgt = base.plusMinutes(1);
        JSONObject body = new JSONObject()
                .put("email","u@test.local")
                .put("message","hello")
                .put("date", tgt.toLocalDate().toString())
                .put("time", tgt.toLocalTime().format(HM));
        StubHttpServletResponse resp = new StubHttpServletResponse();
        s.doPost(new StubHttpServletRequest(body.toString()), resp);
        assertTrue(resp.getStatus() >= 200 && resp.getStatus() < 300);
        assertTrue(containsAny(resp.getBody(), "scheduled"));
    }

    private static boolean containsAny(String text, String... keys) {
        String t = Objects.toString(text, "").toLowerCase();
        for (String k : keys) if (k != null && t.contains(k.toLowerCase())) return true;
        return false;
        }
}
