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

          
            final String url    = System.getenv("DB_URL");
            final String dbUser = System.getenv("DB_USER");
            final String dbPass = System.getenv("DB_PASS");

            if (isBlank(url) || isBlank(dbUser) || isBlank(dbPass)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                result.put("status", "error")
                      .put("message", "Database configuration is missing (DB_URL/DB_USER/DB_PASS).");
                response.getWriter().write(result.toString());
                return;
            }

            
            Class.forName("com.mysql.cj.jdbc.Driver");

          
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

                
                String insertSql = "INSERT INTO users(username, password) VALUES(?, ?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    ins.setString(1, username);
                    ins.setString(2, password);
                    ins.executeUpdate();
                }

                result.put("status", "success");
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            result.put("status", "fail").put("message", "Username already exists.");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "error").put("message", e.getMessage());
        }

        response.getWriter().write(result.toString());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
