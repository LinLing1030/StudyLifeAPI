package com.studylife.servlet;

import org.json.JSONObject;
import org.json.JSONException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.stream.Collectors;

public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private void setCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setCors(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCors(response);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        // 读取请求体
        final String body;
        try (BufferedReader br = request.getReader()) {
            body = br.lines().collect(Collectors.joining());
        }

        final JSONObject result = new JSONObject();

       
        final JSONObject json;
        try {
            json = new JSONObject(body);
        } catch (JSONException ex) {
           
            result.put("status", "error").put("message", "Invalid JSON");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, result.toString());
            return;
        }

        final String username = json.optString("username", "").trim();
        final String password = json.optString("password", "").trim();

        if (username.isEmpty() || password.isEmpty()) {
            result.put("status", "error").put("message", "Username or password empty.");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, result.toString());
            return;
        }

        Integer userId = null;

        
        try {
            userId = authenticateFromDb(username, password);
        } catch (Exception ignored) {
            userId = null;
        }

        
        if (userId == null) {
            userId = fallbackAuth(username, password); // 123/123 -> 1, 456/456 -> 3
        }

        if (userId != null) {
            JSONObject ok = new JSONObject()
                    .put("status", "success")
                    .put("userId", userId)
                    .put("username", username);
            writeJson(response, HttpServletResponse.SC_OK, ok.toString());
        } else {
            JSONObject fail = new JSONObject()
                    .put("status", "fail")
                    .put("message", "Invalid credentials.");
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, fail.toString());
        }
    }

    
    private Integer authenticateFromDb(String username, String password) throws Exception {
        
        String url  = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");

        if (url == null || user == null || pass == null) {
            return null;
        }

        
        Class.forName("com.mysql.cj.jdbc.Driver");

        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            return null;
        }
    }

   
    private Integer fallbackAuth(String username, String password) {
        if ("123".equals(username) && "123".equals(password)) return 1;
        if ("456".equals(username) && "456".equals(password)) return 3;
        return null;
    }

    private void writeJson(HttpServletResponse resp, int status, String body) throws IOException {
        resp.setStatus(status);
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(body);
            pw.flush();
        }
    }
}
