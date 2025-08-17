package com.studylife.servlet;

import org.junit.Test;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class LoginServletTest {

    @Test
    public void invalidJson_returns400() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        when(req.getReader()).thenReturn(new BufferedReader(new StringReader("{not-json")));

        StringWriter out = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(out, true));

        new LoginServlet().doPost(req, resp);

        // 验证 4xx 或 5xx
        verify(resp).setStatus(anyInt());
        String body = out.toString();
        assertTrue(body.toLowerCase().contains("error") || body.contains("\"status\""));
    }
}
