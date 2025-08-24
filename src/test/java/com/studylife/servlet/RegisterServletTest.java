package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


public class RegisterServletTest {

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

        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body.toString())));

        StringWriter outBuf = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(outBuf, true));

        new RegisterServlet().doPost(req, resp);

        String result = outBuf.toString().toLowerCase();
        assertTrue("expect fail message when username/password empty, body=" + result,
                result.contains("fail") ||
                result.contains("username or password empty") ||
                result.contains("\"status\""));
    }

    @Test
    public void missingDbEnv_shouldReturn500_withErrorMessage() throws Exception {
        JSONObject body = new JSONObject()
                .put("username", "bob")
                .put("password", "pass@pwd");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body.toString())));

        StringWriter outBuf = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(outBuf, true));

        new RegisterServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        String result = outBuf.toString().toLowerCase();
        assertTrue("expect message about missing db config, body=" + result,
                result.contains("database configuration is missing")
                        || result.contains("\"status\"")
                        || result.contains("\"error\""));
    }
}
