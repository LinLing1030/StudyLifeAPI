package com.studylife.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.*;
import org.json.JSONException;
import org.json.JSONObject;

@WebServlet("/SendReminderServlet")
public class SendReminderServlet extends HttpServlet {

    /* ===== 常量 ===== */
    private static final String CT_JSON_UTF8 = "application/json;charset=UTF-8";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";
    private static final ZoneId ZONE = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* 线程池：如在容器里长期运行，建议配合 ContextListener 优雅关闭 */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(5);

    /* ===== 小工具：安全 put + 统一写出 JSON ===== */
    private static void safePut(JSONObject obj, String key, Object val) {
        try { obj.put(key, val); } catch (JSONException ignored) {}
    }

    private static void writeJson(HttpServletResponse resp, int httpCode, JSONObject json) {
        resp.setStatus(httpCode);
        resp.setContentType(CT_JSON_UTF8);
        try (PrintWriter out = resp.getWriter()) {
            out.write(json.toString());
        } catch (IOException ignored) { /* 网络中断等，不再额外处理 */ }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // setCharacterEncoding 可能抛 UnsupportedEncodingException
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            JSONObject res = new JSONObject();
            safePut(res, FIELD_STATUS, STATUS_ERROR);
            safePut(res, FIELD_MESSAGE, "Unsupported request encoding");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, res);
            return;
        }

        // 读取请求体（getReader / readLine 可能抛 IOException）
        final String body;
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder(256);
            String line;
            while ((line = reader.readLine()) != null) {  // readLine 可能抛 IOException
                sb.append(line);
            }
            body = sb.toString();
        } catch (IOException e) {
            JSONObject res = new JSONObject();
            safePut(res, FIELD_STATUS, STATUS_ERROR);
            safePut(res, FIELD_MESSAGE, "Failed to read request body");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, res);
            return;
        }

        // 解析 JSON
        final String email;
        final String timeStr; // HH:mm
        final String dateStr; // yyyy-MM-dd
        final String msg;
        try {
            JSONObject json = new JSONObject(body);
            email   = json.optString("email", "").trim();
            timeStr = json.optString("time", "").trim();
            dateStr = json.optString("date", "").trim();
            msg     = json.optString("message", "").trim();
        } catch (Exception ex) {
            JSONObject res = new JSONObject();
            safePut(res, FIELD_STATUS, STATUS_ERROR);
            safePut(res, FIELD_MESSAGE, "Invalid JSON");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, res);
            return;
        }

        if (email.isEmpty() || timeStr.isEmpty() || dateStr.isEmpty() || msg.isEmpty()) {
            JSONObject res = new JSONObject();
            safePut(res, FIELD_STATUS, STATUS_ERROR);
            safePut(res, FIELD_MESSAGE, "Missing required fields");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, res);
            return;
        }

        // 解析日期时间并计算延迟
        final long delayMillis;
        final String createdAt;
        try {
            LocalDate selectedDate = LocalDate.parse(dateStr);        // 可能抛 DateTimeParseException
            LocalTime selectedTime = LocalTime.parse(timeStr);        // 可能抛 DateTimeParseException
            ZonedDateTime target   = ZonedDateTime.of(selectedDate, selectedTime, ZONE);
            ZonedDateTime now      = ZonedDateTime.now(ZONE);

            if (target.isBefore(now)) {
                JSONObject res = new JSONObject();
                safePut(res, FIELD_STATUS, STATUS_ERROR);
                safePut(res, FIELD_MESSAGE, "Selected time has already passed.");
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, res);
                return;
            }

            delayMillis = Duration.between(now, target).toMillis();
            createdAt   = now.format(TS_FMT);
        } catch (DateTimeParseException dtpe) {
            JSONObject res = new JSONObject();
            safePut(res, FIELD_STATUS, STATUS_ERROR);
            safePut(res, FIELD_MESSAGE, "Invalid date/time format");
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, res);
            return;
        } catch (Exception e) {
            JSONObject res = new JSONObject();
            safePut(res, FIELD_STATUS, STATUS_ERROR);
            safePut(res, FIELD_MESSAGE, "Failed to compute schedule");
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res);
            return;
        }

        // 组装提醒内容
        String fullMessage = "⏰ Reminder at " + dateStr + " " + timeStr + ":\n\n" + msg;

        // 调度任务（EmailUtil.sendEmail 可能抛异常，但我们在线程中自行捕获）
        SCHEDULER.schedule(() -> {
            try {
                EmailUtil.sendEmail(email, "Reminder Alert", fullMessage);
            } catch (Exception e) {
                // 这里仅记录（可替换为日志）
                e.printStackTrace();
            }
        }, delayMillis, TimeUnit.MILLISECONDS);

        // 成功返回
        JSONObject ok = new JSONObject();
        safePut(ok, FIELD_STATUS, STATUS_SUCCESS);
        safePut(ok, FIELD_CREATED_AT, createdAt);
        writeJson(response, HttpServletResponse.SC_OK, ok);
    }
}
