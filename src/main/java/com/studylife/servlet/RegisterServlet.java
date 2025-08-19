package com.studylife.servlet;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.util.stream.Collectors;

public class RegisterServlet extends HttpServlet {
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

            // 1) 查重
            String checkSql = "SELECT id FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, username);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            result.put("status", "fail").put("message", "Username already exists.");
                            response.getWriter().write(result.toString());
                            return;
                        }
                    }
                }

                // 2) 插入
                String insertSql = "INSERT INTO users(username, password) VALUES(?, ?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    ins.setString(1, username);
                    ins.setString(2, password); // 如需加密，这里替换为哈希后的密码
                    ins.executeUpdate();
                }

                result.put("status", "success");
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            result.put("status", "fail").put("message", "Username already exists.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error").put("message", e.getMessage());
        }

        response.getWriter().write(result.toString());
    }
}
