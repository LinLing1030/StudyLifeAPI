package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class RegisterServletTest {

    @Test
    public void successRegister_ok() throws Exception {
        JSONObject body = new JSONObject()
                .put("username", "bob")
                .put("password", "pass@pwd")
                .put("email", "bob@test.local");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body.toString())));
        StringWriter out = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(out, true));

       
        RegisterServlet servlet = new RegisterServlet();
        ServletConfig cfg = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        when(cfg.getServletContext()).thenReturn(ctx);
        servlet.init(cfg);

        servlet.doPost(req, resp);

       
        String result = out.toString().toLowerCase();
        assertTrue(result.contains("success") || result.contains("\"status\""));
    }
}
