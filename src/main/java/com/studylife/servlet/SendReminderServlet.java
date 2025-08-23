package com.studylife.servlet;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.*;

@WebServlet("/SendReminderServlet")
public class SendReminderServlet extends HttpServlet {

    // 固定大小线程池：负责定时任务
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(5);

    // 统一时区 & 格式
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TS_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        addCors(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addCors(response);
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String body = readBody(request);

        try (PrintWriter out = response.getWriter()) {
            JSONObject json = new JSONObject(body);

         
            String email   = json.optString("email", "").trim();
            String dateStr = json.optString("date", "").trim(); // yyyy-MM-dd
            String timeStr = json.optString("time", "").trim(); // HH:mm
            String message = json.optString("message", "").trim();

            if (email.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty() || message.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Missing required fields\"}");
                return;
            }

            LocalDate date;
            LocalTime time;
            try {
                date = LocalDate.parse(dateStr, DATE_FMT);
                time = LocalTime.parse(timeStr, TIME_FMT);
            } catch (DateTimeParseException ex) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Invalid date or time format\"}");
                return;
            }

            
            ZonedDateTime nowZ = ZonedDateTime.now(ZONE_ID);
            ZonedDateTime targetZ = ZonedDateTime.of(date, time, ZONE_ID);

            if (targetZ.isBefore(nowZ)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"status\":\"error\",\"message\":\"Selected time has already passed.\"}");
                return;
            }

            long delayMillis = Duration.between(nowZ, targetZ).toMillis();

            
            final String subject = "Reminder Alert";
            final String fullMsg = "⏰ Reminder at " + dateStr + " " + timeStr + ":\n\n" + message;

            SCHEDULER.schedule(() -> {
                try {
                    EmailUtil.sendEmail(email, subject, fullMsg);
                    System.out.println("[Reminder] SENT -> " + email + " at " + TS_FMT.format(targetZ));
                } catch (Exception e) {
                    System.err.println("[Reminder] FAILED -> " + email + " at " + TS_FMT.format(targetZ)
                            + " : " + e.getMessage());
                    e.printStackTrace();
                }
            }, delayMillis, TimeUnit.MILLISECONDS);

            
            JSONObject res = new JSONObject()
                    .put("status", "success")
                    .put("scheduledFor", TS_FMT.format(targetZ))
                    .put("now", TS_FMT.format(nowZ))
                    .put("mode", "scheduled");
            response.setStatus(HttpServletResponse.SC_OK);
            out.print(res.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = response.getWriter()) {
                out.print("{\"status\":\"error\",\"message\":\"" +
                        e.getMessage().replace("\"", "\\\"") + "\"}");
            }
        }
    }

   
    @Override
    public void destroy() {
        SCHEDULER.shutdown();
        try {
            if (!SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
        super.destroy();
    }

    
    private static void addCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
