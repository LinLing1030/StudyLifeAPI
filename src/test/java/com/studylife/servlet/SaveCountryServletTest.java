package com.studylife.servlet;

import org.junit.Test;
import testsupport.StubHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class SaveCountryServletTest {

    private static HttpServletRequest reqWithBody(String body) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
        return req;
    }

    private static StubHttpServletResponse stubResp() {
        return new StubHttpServletResponse();
    }

    private static class TestableServlet extends SaveCountryServlet {
        long idToReturn = 42L;
        Exception toThrow = null;
        DbConf conf = new DbConf("jdbc:fake://x", "u", "p");

        @Override protected DbConf getDbConf() { return conf; }

        @Override
        protected long insertLocation(int userId, String country, DbConf conf) throws Exception {
            if (toThrow != null) throw toThrow;
            return idToReturn;
        }
    }

    private static class JdbcServlet extends SaveCountryServlet {
        private final DbConf conf;
        JdbcServlet(DbConf conf) { this.conf = conf; }
        @Override protected DbConf getDbConf() { return conf; }
        @Override
        protected long insertLocation(int userId, String country, DbConf conf) throws Exception {
            return super.insertLocation(userId, country, conf);
        }
    }

    @Test
    public void options_returns204() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = mock(HttpServletRequest.class);
        StubHttpServletResponse resp = stubResp();
        s.doOptions(req, resp);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, resp.getStatus());
    }

    @Test
    public void badJson_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = reqWithBody("{bad");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().toLowerCase().contains("malformed json"));
    }

    @Test
    public void missingFields_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = reqWithBody("{\"userId\":\"\",\"country\":\"\"}");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().toLowerCase().contains("required"));
    }

    @Test
    public void userIdNotNumber_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = reqWithBody("{\"userId\":\"abc\",\"country\":\"IE\"}");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().toLowerCase().contains("must be a number"));
    }

    @Test
    public void envMissing_returns500() throws Exception {
        TestableServlet s = new TestableServlet();
        s.conf = new SaveCountryServlet.DbConf(null, "u", "p");

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(500, resp.getStatus());
        assertTrue(resp.getBody().toLowerCase().contains("not set"));
    }

    @Test
    public void success_returns200_andBody() throws Exception {
        TestableServlet s = new TestableServlet();
        s.idToReturn = 999L;

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(200, resp.getStatus());
        String body = resp.getBody();
        assertTrue(body.contains("\"success\""));
        assertTrue(body.contains("\"id\":999"));
        assertTrue(body.contains("\"country\":\"IE\""));
    }

    @Test
    public void duplicate_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        s.toThrow = new SQLIntegrityConstraintViolationException("dup");

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().toLowerCase().contains("duplicate"));
    }

    @Test
    public void sqlError_returns500() throws Exception {
        TestableServlet s = new TestableServlet();
        s.toThrow = new SQLException("boom");

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(500, resp.getStatus());
        assertTrue(resp.getBody().toLowerCase().contains("database error"));
    }

    @Test
    public void jdbcPath_inserts_andReturnsGeneratedId() throws Exception {
        long generatedId = 321L;
        Driver d = new FakeDriver(generatedId);
        DriverManager.registerDriver(d);
        try {
            JdbcServlet s = new JdbcServlet(new SaveCountryServlet.DbConf("jdbc:fake://mem", "u", "p"));

            HttpServletRequest req = reqWithBody("{\"userId\":\"7\",\"country\":\"CN\"}");
            StubHttpServletResponse resp = stubResp();

            s.doPost(req, resp);

            assertEquals(200, resp.getStatus());
            String body = resp.getBody();
            assertTrue(body.contains("\"success\""));
            assertTrue(body.contains("\"id\":" + generatedId));
            assertTrue(body.contains("\"country\":\"CN\""));
            assertTrue(body.contains("\"userId\":7"));
        } finally {
            DriverManager.deregisterDriver(d);
        }
    }


    private static class FakeDriver implements Driver {
        private final long id;

        FakeDriver(long id) { this.id = id; }

        @Override public Connection connect(String url, Properties info) {
            if (!acceptsURL(url)) return null;
            return connectionProxy(id);
        }

        @Override public boolean acceptsURL(String url) { return url != null && url.startsWith("jdbc:fake"); }
        @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
    }

    private static Connection connectionProxy(long generatedId) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("prepareStatement")) {
                        return preparedStatementProxy(generatedId);
                    }
                    if (name.equals("close")) return null;
                    if (name.equals("isClosed")) return false;
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) return false;
                    if (rt.equals(int.class)) return 0;
                    if (rt.equals(long.class)) return 0L;
                    return null;
                }
        );
    }

    private static PreparedStatement preparedStatementProxy(long generatedId) {
        return (PreparedStatement) java.lang.reflect.Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("setInt") || name.equals("setString")) return null;
                    if (name.equals("executeUpdate")) return 1;
                    if (name.equals("getGeneratedKeys")) return resultSetProxy(generatedId);
                    if (name.equals("close")) return null;
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) return false;
                    if (rt.equals(int.class)) return 0;
                    if (rt.equals(long.class)) return 0L;
                    return null;
                }
        );
    }

    private static ResultSet resultSetProxy(long id) {
        return (ResultSet) java.lang.reflect.Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                new java.lang.reflect.InvocationHandler() {
                    boolean yielded = false;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        String name = method.getName();
                        if (name.equals("next")) {
                            if (!yielded) { yielded = true; return true; }
                            return false;
                        }
                        if (name.equals("getLong")) return id;
                        if (name.equals("close")) return null;
                        Class<?> rt = method.getReturnType();
                        if (rt.equals(boolean.class)) return false;
                        if (rt.equals(int.class)) return 0;
                        if (rt.equals(long.class)) return 0L;
                        return null;
                    }
                }
        );
    }
}
