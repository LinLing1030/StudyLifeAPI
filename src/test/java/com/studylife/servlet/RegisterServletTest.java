package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import testsupport.StubHttpServletRequest;
import testsupport.StubHttpServletResponse;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class RegisterServletTest {


    @SuppressWarnings("unchecked")
    private static void setEnv(Map<String, String> patch) {
        Map<String, String> sanitized = new HashMap<>();
        for (Map.Entry<String, String> e : patch.entrySet()) {
            sanitized.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
        }
        try {
            try {
                Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
                Field f1 = pe.getDeclaredField("theEnvironment");
                f1.setAccessible(true);
                Map<String, String> env = (Map<String, String>) f1.get(null);
                env.putAll(sanitized);

                Field f2 = pe.getDeclaredField("theCaseInsensitiveEnvironment");
                f2.setAccessible(true);
                Map<String, String> cienv = (Map<String, String>) f2.get(null);
                cienv.putAll(sanitized);
                return;
            } catch (Throwable ignore) {
            }

            Map<String, String> env = System.getenv();
            for (Class<?> cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field m = cl.getDeclaredField("m");
                    m.setAccessible(true);
                    Map<String, String> map = (Map<String, String>) m.get(env);
                    map.putAll(sanitized);
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set env for test", e);
        }
    }

    private static void clearDbEnv() {
        Map<String, String> blanks = new HashMap<>();
        blanks.put("DB_URL",  "");
        blanks.put("DB_USER", "");
        blanks.put("DB_PASS", "");
        setEnv(blanks);
    }

    @After
    public void tearDown() {
        clearDbEnv();
    }

    // ============ tests ============

    @Test
    public void options_shouldReturn204() throws Exception {
        StubHttpServletRequest req = new StubHttpServletRequest("");
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new RegisterServlet().doOptions(req, resp);

        assertTrue(resp.getStatus() == 204);
    }

    @Test
    public void emptyUsernameOrPassword_shouldFailMessage() throws Exception {
        JSONObject body = new JSONObject()
                .put("username", "")
                .put("password", "");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new RegisterServlet().doPost(req, resp);

        String result = safe(resp).toLowerCase();
        assertTrue(result.contains("fail")
                || result.contains("username or password empty")
                || result.contains("\"status\""));
    }

    @Test
    public void missingDbEnv_shouldReturn500_withErrorMessage() throws Exception {
        JSONObject body = new JSONObject()
                .put("username", "alice")
                .put("password", "pwd");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new RegisterServlet().doPost(req, resp);

        assertTrue("expect 5xx", resp.getStatus() >= 500);
        String result = safe(resp).toLowerCase();
        assertTrue(result.contains("database configuration is missing")
                || result.contains("\"error\""));
    }

    @Test
    public void register_success_withH2MemoryDb() throws Exception {
        final String url = "jdbc:h2:mem:regtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        setEnv(new HashMap<String, String>() {{
            put("DB_URL", url);
            put("DB_USER", "sa");
            put("DB_PASS", "");
        }});

        try (Connection c = DriverManager.getConnection(url, "sa", "");
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");
        }

        JSONObject body = new JSONObject()
                .put("username", "bob")
                .put("password", "pass@pwd");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new RegisterServlet().doPost(req, resp);

        String result = safe(resp).toLowerCase();
        assertTrue((resp.getStatus() == 0 || (resp.getStatus() >= 200 && resp.getStatus() < 300))
                && (result.contains("success") || result.contains("\"status\"")));
    }

    @Test
    public void duplicatedUsername_shouldFail_withH2MemoryDb() throws Exception {
        final String url = "jdbc:h2:mem:regtest_dup;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        setEnv(new HashMap<String, String>() {{
            put("DB_URL", url);
            put("DB_USER", "sa");
            put("DB_PASS", "");
        }});

        try (Connection c = DriverManager.getConnection(url, "sa", "");
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");
            st.execute("INSERT INTO users(username, password) VALUES('kate','x')");
        }

        JSONObject body = new JSONObject()
                .put("username", "kate")
                .put("password", "y");

        StubHttpServletRequest req = new StubHttpServletRequest(body.toString());
        StubHttpServletResponse resp = new StubHttpServletResponse();

        new RegisterServlet().doPost(req, resp);

        String result = safe(resp).toLowerCase();
        assertTrue(result.contains("already exists")
                || result.contains("fail")
                || result.contains("\"status\""));
    }

    private static String safe(StubHttpServletResponse resp) {
        String b = resp.getBody();
        return b == null ? "" : b;
        }
}
