package com.studylife.servlet;

import org.json.JSONObject;
import org.json.JSONException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
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
        response.setContentType("application/json;charset=UTF-8");

        String body;
        try (BufferedReader br = request.getReader()) {
            body = br.lines().collect(Collectors.joining());
        }

        JSONObject result = new JSONObject();

        JSONObject json;
        try {
            json = new JSONObject(body == null ? "" : body);
        } catch (JSONException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            result.put("status", "fail").put("message", "Invalid JSON");
            response.getWriter().write(result.toString());
            return;
        }

        String username = json.optString("username", "").trim();
        String password = json.optString("password", "").trim();

        if (username.isEmpty() || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
            result.put("status", "fail").put("message", "Username or password empty.");
            response.getWriter().write(result.toString());
            return;
        }

        // 从环境变量读取数据库配置（/etc/environment 已设置）
        final String url    = System.getenv("DB_URL");
        final String dbUser = System.getenv("DB_USER");
        final String dbPass = System.getenv("DB_PASS");

        if (isBlank(url) || isBlank(dbUser) || isBlank(dbPass)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            result.put("status", "error")
                  .put("message", "DB configuration is missing (DB_URL / DB_USER / DB_PASS).");
            response.getWriter().write(result.toString());
            return;
        }

        try {
           
            Class.forName("com.mysql.cj.jdbc.Driver");

            String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        response.setStatus(HttpServletResponse.SC_OK); 
                        result.put("status", "success");
                        result.put("userId", rs.getInt("id"));
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
                        result.put("status", "fail").put("message", "Invalid credentials.");
                    }
                }
            }
        } catch (Exception e) {
            
            System.err.println("Login error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
            result.put("status", "error").put("message", "Server error.");
        }

        response.getWriter().write(result.toString());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
