package com.studylife.servlet;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "LoginServlet", urlPatterns = {"/api/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setCors(resp);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        final String body;
        try (BufferedReader br = req.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            body = sb.toString();
        } catch (IOException ioe) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    jsonError("Invalid request body"));
            return;
        }

        final JSONObject json;
        try {
            json = new JSONObject(body);
        } catch (Exception ex) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    jsonError("Malformed JSON"));
            return;
        }

        String username = json.optString("username", null);
        String password = json.optString("password", null);
        if (username == null || password == null) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                    jsonError("Missing username or password"));
            return;
        }

        try {
            
            Integer userId = authenticateFromDb(username, password);

            
            if (userId == null) {
                userId = fallbackAuth(username, password); // 123/123 -> 1, 456/456 -> 3
            }

            if (userId == null) {
                writeJson(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        new JSONObject()
                                .put("status", "fail")
                                .put("message", "Invalid username or password")
                                .toString());
                return;
            }

            JSONObject ok = new JSONObject()
                    .put("status", "success")
                    .put("userId", userId)
                    .put("username", username);
            writeJson(resp, HttpServletResponse.SC_OK, ok.toString());

        } catch (Exception ex) {
            
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    jsonError("Internal server error"));
        }
    }

    private Integer authenticateFromDb(String username, String password) {
        String url = "jdbc:mysql://localhost:3306/studylife_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
        String user = "root";
        String pass = "RootRoot##";

        String sql = "SELECT id FROM users WHERE username=? AND password=?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
            return null;

        } catch (Exception e) {
            
            return null;
        }
    }

   
    private Integer fallbackAuth(String username, String password) {
        if ("123".equals(username) && "123".equals(password)) return 1;
        if ("456".equals(username) && "456".equals(password)) return 3;
        return null;
    }

    private void setCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }

    private void writeJson(HttpServletResponse resp, int status, String body) {
        resp.setStatus(status);
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(body);
            pw.flush();
        } catch (IOException ignored) {}
    }

    private static String jsonError(String message) {
        return new JSONObject()
                .put("status", "error")
                .put("message", message)
                .toString();
    }
}
