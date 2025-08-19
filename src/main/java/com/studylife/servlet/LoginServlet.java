package com.studylife.servlet;

import org.json.JSONObject;

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

        // 读取 JSON 请求体
        String body;
        try (BufferedReader br = request.getReader()) {
            body = br.lines().collect(Collectors.joining());
        }

        JSONObject result = new JSONObject();
        try {
            JSONObject json = new JSONObject(body);
            String username = json.optString("username", "").trim();
            String password = json.optString("password", "").trim();

            if (username.isEmpty() || password.isEmpty()) {
                result.put("status", "fail").put("message", "Username or password empty.");
                response.getWriter().write(result.toString());
                return;
            }

            // 加载驱动（新版本通常可省略，但保留兼容）
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://127.0.0.1:3306/studylife_db"
                    + "?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
            String dbUser = "studyuser";
            String dbPass = "Study2025!";

            String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

            // try-with-resources 自动关闭
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result.put("status", "success");
                        result.put("userId", rs.getInt("id"));
                    } else {
                        result.put("status", "fail").put("message", "Invalid credentials.");
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
