package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LoginServletTest {

    private static HttpServletResponse mockRespWithBody(StringWriter out) throws IOException {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(out, true));
        return resp;
    }

    @Test
    public void badJson_returns400() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{bad-json")));

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        // 400
        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String body = out.toString();
        JSONObject obj = new JSONObject(body);
        assertEquals("error", obj.getString("status"));
        assertTrue(obj.getString("message").toLowerCase().contains("json"));
    }

    @Test
    public void wrongPassword_returns401() throws Exception {
        // 不存在的用户/密码
        JSONObject in = new JSONObject().put("username", "abc").put("password", "xyz");
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(in.toString())));

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        JSONObject obj = new JSONObject(out.toString());
        assertEquals("fail", obj.getString("status"));
    }

    @Test
    public void fallbackLogin_123_123_returns200_withUserId1() throws Exception {
        // 触发 fallback：123/123 -> userId 1
        JSONObject in = new JSONObject().put("username", "123").put("password", "123");
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(in.toString())));

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject(out.toString());
        assertEquals("success", obj.getString("status"));
        assertEquals(1, obj.getInt("userId"));
        assertEquals("123", obj.getString("username"));
    }
}
