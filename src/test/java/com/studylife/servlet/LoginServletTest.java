package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LoginServletTest {

    private static HttpServletResponse mockRespWithBody(StringWriter out) throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(out, true));
        return resp;
    }

    private static HttpServletRequest reqWithJson(JSONObject obj) throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(obj.toString())));
        return req;
    }

    @Test
    public void doOptions_shouldSetCors_and204() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        new LoginServlet().doOptions(req, resp);

        // 204
        verify(resp).setStatus(HttpServletResponse.SC_NO_CONTENT);
        // CORS 头存在（值由被测代码设置，不强行校验具体字符串，避免和实现细节耦合）
        verify(resp).setHeader(eq("Access-Control-Allow-Origin"), anyString());
        verify(resp).setHeader(eq("Access-Control-Allow-Methods"), anyString());
        verify(resp).setHeader(eq("Access-Control-Allow-Headers"), anyString());
    }

    @Test
    public void badJson_returns400() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{bad-json")));

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JSONObject obj = new JSONObject(out.toString());
        assertEquals("error", obj.getString("status"));
        assertTrue(obj.getString("message").toLowerCase().contains("json"));
    }

    @Test
    public void emptyUsernameOrPassword_returns400_andErrorBody() throws Exception {
        // username 空
        JSONObject in1 = new JSONObject().put("username", "").put("password", "p");
        HttpServletRequest req1 = reqWithJson(in1);
        StringWriter out1 = new StringWriter();
        HttpServletResponse resp1 = mockRespWithBody(out1);

        new LoginServlet().doPost(req1, resp1);
        verify(resp1).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JSONObject obj1 = new JSONObject(out1.toString());
        assertEquals("error", obj1.getString("status"));

        // password 空
        JSONObject in2 = new JSONObject().put("username", "u").put("password", "");
        HttpServletRequest req2 = reqWithJson(in2);
        StringWriter out2 = new StringWriter();
        HttpServletResponse resp2 = mockRespWithBody(out2);

        new LoginServlet().doPost(req2, resp2);
        verify(resp2).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JSONObject obj2 = new JSONObject(out2.toString());
        assertEquals("error", obj2.getString("status"));
    }

    @Test
    public void wrongPassword_returns401() throws Exception {
        JSONObject in = new JSONObject().put("username", "abc").put("password", "xyz");
        HttpServletRequest req = reqWithJson(in);

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        JSONObject obj = new JSONObject(out.toString());
        assertEquals("fail", obj.getString("status"));
    }

    @Test
    public void fallbackLogin_123_123_returns200_withUserId1() throws Exception {
        JSONObject in = new JSONObject().put("username", "123").put("password", "123");
        HttpServletRequest req = reqWithJson(in);

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject(out.toString());
        assertEquals("success", obj.getString("status"));
        assertEquals(1, obj.getInt("userId"));
        assertEquals("123", obj.getString("username"));
    }

    @Test
    public void fallbackLogin_456_456_returns200_withUserId3() throws Exception {
        JSONObject in = new JSONObject().put("username", "456").put("password", "456");
        HttpServletRequest req = reqWithJson(in);

        StringWriter out = new StringWriter();
        HttpServletResponse resp = mockRespWithBody(out);

        new LoginServlet().doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        JSONObject obj = new JSONObject(out.toString());
        assertEquals("success", obj.getString("status"));
        assertEquals(3, obj.getInt("userId"));
        assertEquals("456", obj.getString("username"));
    }
}
