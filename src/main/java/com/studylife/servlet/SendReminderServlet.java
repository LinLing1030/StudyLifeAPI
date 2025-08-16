package com.studylife.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import org.json.JSONObject;

@WebServlet("/SendReminderServlet")
public class SendReminderServlet extends HttpServlet {

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(5);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        try {
            JSONObject json = new JSONObject(sb.toString());
            String email = json.getString("email");
            String time = json.getString("time"); // 格式: "HH:mm"
            String date = json.getString("date"); // 新增字段 yyyy-MM-dd
            String message = json.getString("message");

            ZoneId zone = ZoneId.of("Europe/Dublin");
            LocalDate selectedDate = LocalDate.parse(date); // 解析前端传来的日期
            LocalTime selectedTime = LocalTime.parse(time);
            LocalDateTime targetDateTime = LocalDateTime.of(selectedDate, selectedTime);

            LocalDateTime now = LocalDateTime.now(zone);

            // 如果选择的时间比当前时间早，不允许（这里简单处理）
            if (targetDateTime.isBefore(now)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Selected time has already passed.\"}");
                out.flush();
                return;
            }

            // 计算延迟时间
            long delayMillis = Duration.between(now, targetDateTime).toMillis();

            String createdAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            String fullMessage =
                    "⏰ Reminder at " + date + " " + time + ":\n\n" +
                    message;

            scheduler.schedule(() -> {
                try {
                    EmailUtil.sendMail(email, "Reminder Alert", fullMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, delayMillis, TimeUnit.MILLISECONDS);

            // 返回 JSON
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("createdAt", createdAt);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(result.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }

        out.flush();
    }
}
