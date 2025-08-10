package com.studylife.servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import org.json.JSONObject;

public class SaveCountryServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置 CORS 响应头
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while((line = reader.readLine()) != null){
                sb.append(line);
            }
        }

        try {
            JSONObject json = new JSONObject(sb.toString());
            String userId = json.getString("userId");
            String country = json.getString("country");

            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/studylife_db?serverTimezone=UTC", "root", "RootRoot##");
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO user_login_locations (user_id, country) VALUES (?, ?)")) {

                ps.setString(1, userId);
                ps.setString(2, country);
                ps.executeUpdate();
            }

            response.getWriter().write("{\"status\":\"success\"}");

        } catch(Exception e){
            e.printStackTrace();
            response.getWriter().write("{\"status\":\"error\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }

    // 处理 OPTIONS 预检请求（CORS最佳实践）
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
