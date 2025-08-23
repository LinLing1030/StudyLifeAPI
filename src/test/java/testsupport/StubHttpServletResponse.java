package testsupport;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.Map;
import java.util.function.Supplier;
/**
 * Minimal, test-only HttpServletResponse stub.
 *
 * Only the methods that are actually useful in unit tests are implemented with
 * sensible behaviour. All the others throw UnsupportedOperationException to
 * avoid silent no-ops (and to satisfy static analyzers like Sonar).
 */
public class StubHttpServletResponse implements HttpServletResponse {

  private int status = SC_OK;
  private final StringWriter bodyWriter = new StringWriter();
  private final PrintWriter writer = new PrintWriter(bodyWriter, true);
  private String contentType = "application/json";
  private String characterEncoding = "UTF-8";
  private boolean committed = false;
  private String redirectLocation;

  /** simple header store for tests */
  private final Map<String, List<String>> headers = new LinkedHashMap<>();

  // ----------------------- helpers for tests -----------------------

  public int getStatus() { return status; }

  public String getBody() { return bodyWriter.toString(); }

  public String getRedirectLocation() { return redirectLocation; }

  // ----------------------- methods with real behaviour -----------------------

  @Override public void setStatus(int sc) { this.status = sc; }

  /** Deprecated in Servlet spec but still part of interface. Do minimal handling. */
  @Override public void setStatus(int sc, String sm) { this.status = sc; }

  @Override public void sendError(int sc, String msg) throws IOException {
    this.status = sc;
    writer.println(msg);
    committed = true;
  }

  @Override public void sendError(int sc) throws IOException {
    this.status = sc;
    committed = true;
  }

  @Override public void sendRedirect(String location) throws IOException {
    this.redirectLocation = location;
    this.status = SC_FOUND;
    setHeader("Location", location);
    committed = true;
  }

  @Override public void setContentType(String type) { this.contentType = type; }

  @Override public String getContentType() { return contentType; }

  @Override public void setCharacterEncoding(String charset) { this.characterEncoding = charset; }

  @Override public String getCharacterEncoding() { return characterEncoding; }

  @Override public PrintWriter getWriter() { return writer; }

  @Override public ServletOutputStream getOutputStream() {
    // Provide a minimal OutputStream that writes into the same body as getWriter()
    return new ServletOutputStream() {
      private final Writer w = writer;
      @Override public boolean isReady() { return true; }
      @Override public void setWriteListener(WriteListener writeListener) { /* not needed in tests */ }
      @Override public void write(int b) throws IOException { w.write(b); }
      @Override public void flush() throws IOException { w.flush(); }
    };
  }

  @Override public void flushBuffer() throws IOException {
    writer.flush();
    committed = true;
  }

  @Override public boolean isCommitted() { return committed; }

  @Override public void resetBuffer() {
    bodyWriter.getBuffer().setLength(0);
  }

  @Override public void reset() {
    resetBuffer();
    headers.clear();
    contentType = "application/json";
    characterEncoding = "UTF-8";
    status = SC_OK;
    redirectLocation = null;
    committed = false;
  }

  // ----------------------- simple header support for tests -----------------------

  @Override public void setHeader(String name, String value) {
    List<String> list = new ArrayList<>(1);
    list.add(value);
    headers.put(name, list);
  }

  @Override public void addHeader(String name, String value) {
    headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
  }

  @Override public boolean containsHeader(String name) {
    return headers.containsKey(name);
  }

  @Override public String getHeader(String name) {
    List<String> list = headers.get(name);
    return (list == null || list.isEmpty()) ? null : list.get(0);
  }

  @Override public Collection<String> getHeaders(String name) {
    List<String> list = headers.get(name);
    return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
  }

  @Override public Collection<String> getHeaderNames() {
    return Collections.unmodifiableSet(headers.keySet());
  }

  // ----------------------- trivial / delegated implementations -----------------------

  @Override public String encodeURL(String url) { return url; }

  @Override public String encodeRedirectURL(String url) { return url; }

  /** Deprecated legacy methods → delegate to non-deprecated ones to avoid empty bodies. */
  @Override @Deprecated public String encodeUrl(String url) { return encodeURL(url); }

  @Override @Deprecated public String encodeRedirectUrl(String url) { return encodeRedirectURL(url); }

  @Override public void addCookie(Cookie cookie) { /* not needed by tests */ }

  @Override public void setDateHeader(String name, long date) { setHeader(name, String.valueOf(date)); }

  @Override public void addDateHeader(String name, long date) { addHeader(name, String.valueOf(date)); }

  @Override public void setIntHeader(String name, int value) { setHeader(name, String.valueOf(value)); }

  @Override public void addIntHeader(String name, int value) { addHeader(name, String.valueOf(value)); }

  @Override public void setContentLength(int len) { /* not needed by tests */ }

  @Override public void setContentLengthLong(long len) { /* not needed by tests */ }

  @Override public void setBufferSize(int size) { /* not needed by tests */ }

  @Override public int getBufferSize() { return 0; }

  @Override public void setLocale(Locale loc) { /* not needed by tests */ }

  @Override public Locale getLocale() { return Locale.getDefault(); }


//HTTP trailers (Servlet 4.0+). Not used in our tests; explicitly fail if called.
@Override
public void setTrailerFields(Supplier<Map<String, String>> supplier) {
   throw new UnsupportedOperationException("Test stub: trailers not implemented");
}

@Override
public Supplier<Map<String, String>> getTrailerFields() {
   throw new UnsupportedOperationException("Test stub: trailers not implemented");
}
}
