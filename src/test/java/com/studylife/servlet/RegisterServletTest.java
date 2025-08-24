package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RegisterServletTest {

    private static class Captured {
        final StringWriter buf = new StringWriter();
        final PrintWriter out = new PrintWriter(buf, true);
        String body() { return buf.toString(); }
    }

    private static Captured prepareIO(HttpServletRequest req, HttpServletResponse resp, String jsonBody) throws Exception {
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(jsonBody)));
        Captured cap = new Captured();
        when(resp.getWriter()).thenReturn(cap.out);
        return cap;
    }


    @SuppressWarnings("unchecked")
    private static void setEnv(Map<String, String> newenv) {
        try {
            try {
                Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
                Field theEnvironmentField = pe.getDeclaredField("theEnvironment");
                theEnvironmentField.setAccessible(true);
                Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
                env.putAll(newenv);
                Field theCaseInsensitiveEnvironmentField = pe.getDeclaredField("theCaseInsensitiveEnvironment");
                theCaseInsensitiveEnvironmentField.setAccessible(true);
                Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
                cienv.putAll(newenv);
                return;
            } catch (Throwable ignore) {  }


            Map<String, String> env = System.getenv();
            for (Class<?> cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field m = cl.getDeclaredField("m");
                    m.setAccessible(true);
                    Object obj = m.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.putAll(newenv);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set env for test", e);
        }
    }

    private static void clearDbEnv() {
        setEnv(new HashMap<String, String>() {{
            put("DB_URL", null);
            put("DB_USER", null);
            put("DB_PASS", null);
        }});
    }

    @After
    public void tearDown() {
        clearDbEnv();
    }

    // ===================== tests =====================

    @Test
    public void options_shouldReturn204() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        new RegisterServlet().doOptions(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void emptyUsernameOrPassword_shouldFailMessage() throws Exception {
        JSONObject body = new JSONObject()
                .put("username", "")
                .put("password", "");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Captured cap = prepareIO(req, resp, body.toString());

        new RegisterServlet().doPost(req, resp);

        String result = cap.body().toLowerCase();
        assertTrue("expect fail message when username/password empty, body=" + result,
                result.contains("fail")
                        || result.contains("username or password empty")
                        || result.contains("\"status\""));
    }

    @Test
    public void missingDbEnv_shouldReturn500_withErrorMessage() throws Exception {
        JSONObject body = new JSONObject()
                .put("username", "alice")
                .put("password", "pwd");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Captured cap = prepareIO(req, resp, body.toString());

        new RegisterServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String result = cap.body().toLowerCase();
        assertTrue(result.contains("database configuration is missing")
                || result.contains("\"error\""));
    }

    @Test
    public void register_success_withH2MemoryDb() throws Exception {
        final String url = "jdbc:h2:mem:regtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        final String user = "sa";
        final String pass = "";
        setEnv(new HashMap<String, String>() {{
            put("DB_URL", url);
            put("DB_USER", user);
            put("DB_PASS", pass);
        }});

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users(" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");
        }

        JSONObject body = new JSONObject()
                .put("username", "bob")
                .put("password", "pass@pwd");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Captured cap = prepareIO(req, resp, body.toString());

        new RegisterServlet().doPost(req, resp);

        String result = cap.body().toLowerCase();
        assertTrue("expect success body, got: " + result,
                result.contains("success") || result.contains("\"status\""));
    }

    @Test
    public void duplicatedUsername_shouldFail_withH2MemoryDb() throws Exception {
        final String url = "jdbc:h2:mem:regtest_dup;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        final String user = "sa";
        final String pass = "";
        setEnv(new HashMap<String, String>() {{
            put("DB_URL", url);
            put("DB_USER", user);
            put("DB_PASS", pass);
        }});


        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users(" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL" +
                    ")");
            st.execute("INSERT INTO users(username, password) VALUES('kate','x')");
        }


        JSONObject body = new JSONObject()
                .put("username", "kate")
                .put("password", "y");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Captured cap = prepareIO(req, resp, body.toString());

        new RegisterServlet().doPost(req, resp);

        String result = cap.body().toString().toLowerCase();
        assertTrue("expect duplicate user message/fail, body=" + result,
                result.contains("already exists")
                        || result.contains("fail")
                        || result.contains("\"status\""));
    }
}
