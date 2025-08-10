package com.studylife.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import org.json.JSONObject;

public class LoginServlet extends HttpServlet {

    // === CORS ===
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
            throws IOException {

        setCors(response);
        response.setContentType("application/json;charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = request.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }

        JSONObject result = new JSONObject();

        try {
            JSONObject json = new JSONObject(sb.toString());
            String username = json.optString("username", "").trim();
            String password = json.optString("password", "").trim();

            if (username.isEmpty() || password.isEmpty()) {
                result.put("status", "fail").put("message", "Username or password empty.");
                response.getWriter().write(result.toString());
                return;
            }

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(
            		"jdbc:mysql://127.0.0.1:3306/studylife_db?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true",
                "studyuser",
                "Study2025!"
            )) {

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM users WHERE username = ? AND password = ?")) {
                    ps.setString(1, username);
                    ps.setString(2, password); // 演示用明文，实际请改为哈希
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            result.put("status", "success");
                            result.put("userId", rs.getInt("id"));
                        } else {
                            result.put("status", "fail").put("message", "Invalid credentials.");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error").put("message", e.getMessage());
        }

        response.getWriter().write(result.toString());
    }
}
