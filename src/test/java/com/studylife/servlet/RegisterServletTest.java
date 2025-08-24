package com.studylife.servlet;

import org.json.JSONObject;
import org.junit.Test;

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
                .put("password", "pass@pwd");

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body.toString())));

        StringWriter out = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(out, true));

        new RegisterServlet().doPost(req, resp);

        String result = out.toString().toLowerCase();
        assertTrue(result.contains("\"status\""));
    }
}
