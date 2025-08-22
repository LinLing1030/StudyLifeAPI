//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.studylife.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

@WebServlet({"/SendReminderServlet"})
public class SendReminderServlet extends HttpServlet {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public SendReminderServlet() {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();

        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }

        try {
            JSONObject json = new JSONObject(sb.toString());
            String email = json.getString("email");
            String time = json.getString("time");
            String date = json.getString("date");
            String message = json.getString("message");
            ZoneId zone = ZoneId.of("Europe/Dublin");
            LocalDate selectedDate = LocalDate.parse(date);
            LocalTime selectedTime = LocalTime.parse(time);
            LocalDateTime targetDateTime = LocalDateTime.of(selectedDate, selectedTime);
            LocalDateTime now = LocalDateTime.now(zone);
            if (targetDateTime.isBefore(now)) {
                response.setStatus(400);
                out.print("{\"status\":\"error\",\"message\":\"Selected time has already passed.\"}");
                out.flush();
                return;
            }

            long delayMillis = Duration.between(now, targetDateTime).toMillis();
            String createdAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String fullMessage = "⏰ Reminder at " + date + " " + time + ":\n\n" + message;
            scheduler.schedule(() -> {
                try {
                    EmailUtil.sendEmail(email, "Reminder Alert", fullMessage);
                } catch (Exception var3) {
                    Exception e = var3;
                    e.printStackTrace();
                }

            }, delayMillis, TimeUnit.MILLISECONDS);
            JSONObject result = new JSONObject();
            result.put("status", "success");
            result.put("createdAt", createdAt);
            response.setStatus(200);
            out.print(result.toString());
        } catch (Exception var22) {
            Exception e = var22;
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }

        out.flush();
    }
}
