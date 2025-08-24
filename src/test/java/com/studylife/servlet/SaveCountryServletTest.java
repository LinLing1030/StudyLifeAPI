package com.studylife.servlet;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SaveCountryServletTest {



    private static HttpServletRequest reqWithBody(String body) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
        return req;
    }

    private static testsupport.StubHttpServletResponse stubResp() {
        return new testsupport.StubHttpServletResponse();
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

    @Test
    public void options_returns204() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = mock(HttpServletRequest.class);
        testsupport.StubHttpServletResponse resp = stubResp();
        s.doOptions(req, resp);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, resp.getStatus());
    }

    @Test
    public void badJson_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = reqWithBody("{bad");
        testsupport.StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().contains("Malformed JSON"));
    }

    @Test
    public void missingFields_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = reqWithBody("{\"userId\":\"\",\"country\":\"\"}");
        testsupport.StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().contains("required"));
    }

    @Test
    public void userIdNotNumber_returns400() throws Exception {
        TestableServlet s = new TestableServlet();
        HttpServletRequest req = reqWithBody("{\"userId\":\"abc\",\"country\":\"IE\"}");
        testsupport.StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().contains("must be a number"));
    }

    @Test
    public void envMissing_returns500() throws Exception {
        TestableServlet s = new TestableServlet();
        
        s.conf = new SaveCountryServlet.DbConf(null, "u", "p");

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        testsupport.StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(500, resp.getStatus());
        assertTrue(resp.getBody().contains("not set"));
    }

    @Test
    public void success_returns200_andBody() throws Exception {
        TestableServlet s = new TestableServlet();
        s.idToReturn = 999L;

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        testsupport.StubHttpServletResponse resp = stubResp();

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
        testsupport.StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(400, resp.getStatus());
        assertTrue(resp.getBody().contains("Duplicate"));
    }

    @Test
    public void sqlError_returns500() throws Exception {
        TestableServlet s = new TestableServlet();
        s.toThrow = new SQLException("boom");

        HttpServletRequest req = reqWithBody("{\"userId\":\"1\",\"country\":\"IE\"}");
        testsupport.StubHttpServletResponse resp = stubResp();

        s.doPost(req, resp);

        assertEquals(500, resp.getStatus());
        assertTrue(resp.getBody().contains("Database error"));
    }
}
