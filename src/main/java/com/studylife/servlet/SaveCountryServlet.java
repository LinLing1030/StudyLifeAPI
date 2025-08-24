package com.studylife.servlet;

import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

@WebServlet(name = "SaveCountryServlet", urlPatterns = {"/api/save-country"})
public class SaveCountryServlet extends HttpServlet {


    protected static final String ENV_DB_URL  = "DB_URL";
    protected static final String ENV_DB_USER = "DB_USER";
    protected static final String ENV_DB_PASS = "DB_PASS";

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        addCorsHeaders(response);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");

        final String body;
        try (BufferedReader r = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            body = sb.toString();
        } catch (IOException ioe) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Invalid request body"));
            return;
        }

        final JSONObject json;
        try {
            json = new JSONObject(body);
        } catch (Exception ex) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Malformed JSON"));
            return;
        }

        final String userIdStr = json.optString("userId", "").trim();
        final String country   = json.optString("country", "").trim();
        if (isBlank(userIdStr) || isBlank(country)) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    fail("userId and country are required"));
            return;
        }

        final int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch (NumberFormatException nfe) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, fail("userId must be a number"));
            return;
        }

        final DbConf conf = getDbConf();
        if (conf.hasBlank()) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("DB_URL/DB_USER/DB_PASS not set"));
            return;
        }

        try {
            long id = insertLocation(userId, country, conf);
            JSONObject ok = new JSONObject()
                    .put("status", "success")
                    .put("id", id)
                    .put("userId", userId)
                    .put("country", country);
            writeJson(response, HttpServletResponse.SC_OK, ok.toString());
        } catch (SQLIntegrityConstraintViolationException dup) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, fail("Duplicate record"));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error"));
        }
    }

    protected DbConf getDbConf() {
        return new DbConf(
                System.getenv(ENV_DB_URL),
                System.getenv(ENV_DB_USER),
                System.getenv(ENV_DB_PASS)
        );
    }


    protected long insertLocation(int userId, String country, DbConf conf) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String sql = "INSERT INTO user_login_locations (user_id, country) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(conf.url, conf.user, conf.pass);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, country);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1L;
    }



    protected static final class DbConf {
        final String url, user, pass;
        DbConf(String url, String user, String pass) { this.url = url; this.user = user; this.pass = pass; }
        boolean hasBlank() { return isBlank(url) || isBlank(user) || isBlank(pass); }
    }

    protected static void addCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }

    protected static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    protected static String error(String msg) {
        return new JSONObject().put("status", "error").put("message", msg).toString();
    }

    protected static String fail(String msg) {
        return new JSONObject().put("status", "fail").put("message", msg).toString();
    }

    protected static void writeJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        try (PrintWriter w = resp.getWriter()) { w.write(json); }
    }
}
