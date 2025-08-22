package com.studylife.servlet;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * POST /api/save-country
 * Body (JSON): { "userId": "123", "country": "Ireland" }
 */
@WebServlet(name = "SaveCountryServlet", urlPatterns = {"/api/save-country"})
public class SaveCountryServlet extends HttpServlet {

    // === 根据你的环境修改这三项 ===
    private static final String JDBC_URL  =
            "jdbc:mysql://localhost:3306/studylife_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PWD  = "RootRoot##";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ----- CORS -----
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // ----- 编码 & 返回类型 -----
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        // ----- 读取请求体 -----
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }

        try {
            // 解析 JSON
            JSONObject json = new JSONObject(body.toString());
            String userId  = json.getString("userId");
            String country = json.getString("country");

            // JDBC 驱动（MySQL 8+）
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 写库
            String sql = "INSERT INTO user_login_locations (user_id, country) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PWD);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.setString(2, country);
                ps.executeUpdate();
            }

            // 成功
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\":\"success\"}");

        } catch (Exception e) {
            // 失败
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            // 注意：生产环境不要把 e.getMessage() 原样返回给前端
            response.getWriter().write(
                    "{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"","\\\"") + "\"}"
            );
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
