package testsupport;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.*;
import java.io.*;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 极简版 HttpServletResponse stub（仅测试用）
 * - 记录 setStatus(code)
 * - 把 getWriter() 写到 StringWriter 里，测试可用 getBody() 读取
 */
public class StubHttpServletResponse implements HttpServletResponse {

    private int status = 200;
    private final StringWriter sw = new StringWriter();
    private final PrintWriter writer = new PrintWriter(sw, true);
    private String contentType = "application/json";
    private String characterEncoding = "UTF-8";

    public int getStatus() { return status; }
    public String getBody() { return sw.toString(); }

    @Override public void setStatus(int sc) { this.status = sc; }
    @Override public void setStatus(int sc, String sm) { this.status = sc; }
    @Override public void sendError(int sc, String msg) { this.status = sc; writer.println(msg); }
    @Override public void sendError(int sc) { this.status = sc; }
    @Override public void sendRedirect(String location) {}
    @Override public void setContentType(String type) { this.contentType = type; }
    @Override public String getContentType() { return contentType; }
    @Override public void setCharacterEncoding(String charset) { this.characterEncoding = charset; }
    @Override public String getCharacterEncoding() { return characterEncoding; }
    @Override public PrintWriter getWriter() { return writer; }

    @Override public ServletOutputStream getOutputStream() { // 不用就给个简单实现
        return new ServletOutputStream() {
            private final Writer w = writer;
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener writeListener) {}
            @Override public void write(int b) throws IOException { w.write(b); }
        };
    }

    // 其余为“空实现/默认实现”，只为编译通过
    @Override public void addCookie(Cookie cookie) {}
    @Override public boolean containsHeader(String name) { return false; }
    @Override public String encodeURL(String url) { return url; }
    @Override public String encodeRedirectURL(String url) { return url; }
    @Override public String encodeUrl(String url) { return url; }
    @Override public String encodeRedirectUrl(String url) { return url; }
    @Override public void setDateHeader(String name, long date) {}
    @Override public void addDateHeader(String name, long date) {}
    @Override public void setHeader(String name, String value) {}
    @Override public void addHeader(String name, String value) {}
    @Override public void setIntHeader(String name, int value) {}
    @Override public void addIntHeader(String name, int value) {}
    @Override public void setContentLength(int len) {}
    @Override public void setContentLengthLong(long len) {}
    @Override public void setBufferSize(int size) {}
    @Override public int getBufferSize() { return 0; }
    @Override public void flushBuffer() throws IOException { writer.flush(); }
    @Override public void resetBuffer() { sw.getBuffer().setLength(0); }
    @Override public boolean isCommitted() { return false; }
    @Override public void reset() { resetBuffer(); status = 200; }
    @Override public void setLocale(Locale loc) {}
    @Override public Locale getLocale() { return Locale.getDefault(); }
    @Override public void setTrailerFields(Supplier<java.util.Map<String,String>> supplier) {}
    @Override public java.util.function.Supplier<java.util.Map<String,String>> getTrailerFields() { return null; }
    public void setContentLanguage(Locale locale) {}
    public void setContentLengthLong(long l, boolean b) {}
    @Override public Collection<String> getHeaderNames() { return java.util.Collections.emptyList(); }
    @Override public String getHeader(String name) { return null; }
    @Override public Collection<String> getHeaders(String name) { return java.util.Collections.emptyList(); }
}
