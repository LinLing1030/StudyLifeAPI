package com.studylife.servlet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;


public class LoginServlet extends HttpServlet {

    /* ===== 常量：字段名 / 文本 ===== */
    private static final String FIELD_STATUS   = "status";
    private static final String FIELD_MESSAGE  = "message";
    private static final String FIELD_USER_ID  = "userId";
    private static final String FIELD_USERNAME = "username";

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAIL    = "fail";
    private static final String STATUS_ERROR   = "error";

    private static final String MSG_INVALID_JSON        = "Invalid JSON";
    private static final String MSG_INVALID_BODY        = "Invalid request body";
    private static final String MSG_INVALID_CREDENTIALS = "Invalid credentials";
    private static final String MSG_SERVER_CONFIG_ERROR = "Server config error";
    private static final String MSG_DB_ERROR            = "Database error";
    private static final String MSG_SERVER_ERROR        = "Server error";

    private static final String CT_JSON_UTF8 = "application/json;charset=UTF-8";

    // 允许来源
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:3000",
        "http://localhost:5500",
        "https://studylife.example"
    );

    /* ===== 工具：安全 put 与统一输出 ===== */
    private static void safePut(JSONObject obj, String k, Object v) {
        try { obj.put(k, v); } catch (JSONException ignored) { /* never mind */ }
    }

    private static void sendJson(HttpServletResponse resp, int httpCode, JSONObject json) {
        resp.setStatus(httpCode);
        resp.setContentType(CT_JSON_UTF8);
        try (PrintWriter out = resp.getWriter()) {
            out.write(json.toString());
        } catch (IOException ignored) { /* 响应流已关闭或网络中断，不再额外处理 */ }
    }

    /* ===== CORS ===== */
    private void setCors(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
        }
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "no-store");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setCors(req, resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /* ===== JNDI DataSource ===== */
    private DataSource getDataSource() throws NamingException {
        Context initCtx = new InitialContext();
        Context envCtx  = (Context) initCtx.lookup("java:/comp/env");
        return (DataSource) envCtx.lookup("jdbc/StudyLife");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        setCors(request, response);
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            JSONObject err = new JSONObject();
            safePut(err, FIELD_STATUS, STATUS_FAIL);
            safePut(err, FIELD_MESSAGE, MSG_INVALID_BODY);
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, err);
            return;
        }

        JSONObject result = new JSONObject();

        // 读取 JSON
        String body;
        try (BufferedReader r = request.getReader()) {
            StringBuilder sb = new StringBuilder(256);
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            body = sb.toString();
        } catch (IOException e) {
            safePut(result, FIELD_STATUS, STATUS_FAIL);
            safePut(result, FIELD_MESSAGE, MSG_INVALID_BODY);
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, result);
            return;
        }

        String username, password;
        try {
            JSONObject json = new JSONObject(body);
            username = json.optString(FIELD_USERNAME, "").trim();
            password = json.optString("password", "");
        } catch (Exception ex) {
            safePut(result, FIELD_STATUS, STATUS_FAIL);
            safePut(result, FIELD_MESSAGE, MSG_INVALID_JSON);
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, result);
            return;
        }

        // 基础校验
        if (username.isEmpty() || password.isEmpty()
                || username.length() < 3 || username.length() > 64
                || password.length() < 6 || password.length() > 128) {
            safePut(result, FIELD_STATUS, STATUS_FAIL);
            safePut(result, FIELD_MESSAGE, MSG_INVALID_CREDENTIALS);
            sendJson(response, HttpServletResponse.SC_OK, result);
            return;
        }

        try {
            DataSource ds = getDataSource();

            final String sql = "SELECT id, password_hash FROM users WHERE username = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        safePut(result, FIELD_STATUS, STATUS_FAIL);
                        safePut(result, FIELD_MESSAGE, MSG_INVALID_CREDENTIALS);
                        sendJson(response, HttpServletResponse.SC_OK, result);
                        return;
                    }

                    long userId = rs.getLong("id");
                    String hash = rs.getString("password_hash");

                    boolean ok = (hash != null && !hash.isEmpty()) && BCrypt.checkpw(password, hash);
                    if (!ok) {
                        safePut(result, FIELD_STATUS, STATUS_FAIL);
                        safePut(result, FIELD_MESSAGE, MSG_INVALID_CREDENTIALS);
                        sendJson(response, HttpServletResponse.SC_OK, result);
                        return;
                    }

                    safePut(result, FIELD_STATUS, STATUS_SUCCESS);
                    safePut(result, FIELD_USER_ID, userId);
                    safePut(result, FIELD_USERNAME, username);
                    sendJson(response, HttpServletResponse.SC_OK, result);
                }
            }
        } catch (NamingException ne) {
            log("JNDI DataSource lookup failed", ne);
            safePut(result, FIELD_STATUS, STATUS_ERROR);
            safePut(result, FIELD_MESSAGE, MSG_SERVER_CONFIG_ERROR);
            sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        } catch (SQLException se) {
            log("DB error", se);
            safePut(result, FIELD_STATUS, STATUS_ERROR);
            safePut(result, FIELD_MESSAGE, MSG_DB_ERROR);
            sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        } catch (Exception e) {
            log("Unexpected error", e);
            safePut(result, FIELD_STATUS, STATUS_ERROR);
            safePut(result, FIELD_MESSAGE, MSG_SERVER_ERROR);
            sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        }
    }
}
