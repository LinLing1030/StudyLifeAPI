package com.studylife.servlet;

import org.junit.Test;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class LoginServletTest {

	@Test
	public void validLogin_returns200() throws Exception {
	    HttpServletRequest req = mock(HttpServletRequest.class);
	    HttpServletResponse resp = mock(HttpServletResponse.class);

	  
	    String body = "{\"username\":\"123\",\"password\":\"123\"}";
	    when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));

	    StringWriter out = new StringWriter();
	    when(resp.getWriter()).thenReturn(new PrintWriter(out, true));

	    new LoginServlet().doPost(req, resp);

	    verify(resp).setStatus(HttpServletResponse.SC_OK);
	    assertTrue(out.toString().contains("\"status\":\"success\""));
	}

	@Test
	public void wrongPassword_returns401() throws Exception {
	    HttpServletRequest req = mock(HttpServletRequest.class);
	    HttpServletResponse resp = mock(HttpServletResponse.class);

	    String body = "{\"username\":\"123\",\"password\":\"xxx\"}";
	    when(req.getReader()).thenReturn(new BufferedReader(new StringReader(body)));

	    StringWriter out = new StringWriter();
	    when(resp.getWriter()).thenReturn(new PrintWriter(out, true));

	    new LoginServlet().doPost(req, resp);

	    verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    assertTrue(out.toString().contains("\"status\":\"fail\""));
	}}
