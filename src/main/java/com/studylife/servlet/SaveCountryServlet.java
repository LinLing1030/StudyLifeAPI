package com.studylife.servlet;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * POST /api/save-country
 * Body (JSON): { "userId": "123", "country": "Ireland" }
 */
@WebServlet(name = "SaveCountryServlet", urlPatterns = {"/api/save-country"})
public class SaveCountryServlet extends HttpServlet {


    private static final String JDBC_URL =
            "jdbc:mysql://127.0.0.1:3306/studylife_db"
            + "?serverTimezone=UTC"
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&useUnicode=true&characterEncoding=utf8";

    private static final String JDBC_USER = "studyuser";
    private static final String JDBC_PWD  = "Study2025!";


    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        addCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCorsHeaders(response);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        try {
         
            JSONObject json = new JSONObject(body.toString());
            String userId  = json.optString("userId", "").trim();
            String country = json.optString("country", "").trim();

            if (userId.isEmpty() || country.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"status\":\"fail\",\"message\":\"userId and country are required\"}");
                return;
            }


            Class.forName("com.mysql.cj.jdbc.Driver");


            String sql = "INSERT INTO user_login_locations (user_id, country) VALUES (?, ?)";
            long generatedId = -1L;
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PWD);
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, userId);
                ps.setString(2, country);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                    }
                }
            }

      
            JSONObject ok = new JSONObject();
            ok.put("status", "success");
            ok.put("id", generatedId);
            ok.put("userId", userId);
            ok.put("country", country);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(ok.toString());

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage() == null ? "internal error" : e.getMessage().replace("\"", "\\\"");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"" + msg + "\"}");
        }
    }

    
    private void addCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}