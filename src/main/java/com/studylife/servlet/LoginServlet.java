package com.studylife.servlet;

import javax.servlet.http.*;
import javax.naming.*;
import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
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

    // 允许的前端来源（按需增减；不要用 *）
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:3000",
        "http://localhost:5500",
        "https://studylife.example"   // TODO: 替换为你的正式域名
    );

    /* ===== CORS ===== */
    private void setCors(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
            // resp.setHeader("Access-Control-Allow-Credentials", "true");
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

    /* ===== JSON 帮助 ===== */
    private static void safePut(JSONObject obj, String k, Object v) {
        try { obj.put(k, v); } catch (JSONException ignore) { /* log if needed */ }
    }

    private static void sendJson(HttpServletResponse resp, int httpCode, JSONObject json) {
        resp.setStatus(httpCode);
        resp.setContentType(CT_JSON_UTF8);
        try (PrintWriter out = resp.getWriter()) {
            out.write(json.toString());
        } catch (IOException ioe) {
            // 最后兜底：无法写出时仅记录
            ioe.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        setCors(request, response);
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            JSONObject result = new JSONObject();
            safePut(result, FIELD_STATUS, STATUS_FAIL);
            safePut(result, FIELD_MESSAGE, MSG_INVALID_BODY);
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST, result);
            return;
        }
        response.setCharacterEncoding("UTF-8");

        JSONObject result = new JSONObject();

        // 读取 JSON body
        final String body;
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

        // 解析 JSON
        final String username;
        final String password;
        try {
            JSONObject json = new JSONObject(body);
            username = json.optString("username", "").trim();
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

        // 业务：校验密码哈希
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

                    // 登录成功
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
