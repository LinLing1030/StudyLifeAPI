package com.studylife.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

@WebServlet("/SendReminderServlet")
public class SendReminderServlet extends HttpServlet {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(5);

    // 复用常量，避免魔法字符串
    private static final String FIELD_STATUS  = "status";
    private static final String FIELD_MESSAGE = "message";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            request.setCharacterEncoding("UTF-8"); // 可能抛 UnsupportedEncodingException，但 UTF-8 总是可用
        } catch (Exception ignore) { /* no-op */ }

        response.setContentType("application/json;charset=UTF-8");

        // 读 Body
        StringBuilder sb = new StringBuilder(256);
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        JSONObject result = new JSONObject();
        try (PrintWriter out = response.getWriter()) {
            JSONObject json = new JSONObject(sb.toString());
            String email   = json.getString("email");
            String timeStr = json.getString("time"); // "HH:mm"
            String dateStr = json.getString("date"); // "yyyy-MM-dd"
            String message = json.getString("message");

            ZoneId zone = ZoneId.of("Europe/Dublin");
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            LocalDateTime target = LocalDateTime.of(date, time);
            LocalDateTime now = LocalDateTime.now(zone);

            if (target.isBefore(now)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put(FIELD_STATUS, "error")
                      .put(FIELD_MESSAGE, "Selected time has already passed.");
                out.print(result.toString());
                return;
            }

            long delayMillis = Duration.between(now, target).toMillis();
            String createdAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String fullMessage = "⏰ Reminder at " + dateStr + " " + timeStr + ":\n\n" + message;

            SCHEDULER.schedule(() -> {
                try {
                    EmailUtil.sendMail(email, "Reminder Alert", fullMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, delayMillis, TimeUnit.MILLISECONDS);

            result.put(FIELD_STATUS, "success").put("createdAt", createdAt);
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(result.toString());

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                result.put(FIELD_STATUS, "error").put(FIELD_MESSAGE, e.getMessage());
                out.print(result.toString());
            }
        }
    }
}
