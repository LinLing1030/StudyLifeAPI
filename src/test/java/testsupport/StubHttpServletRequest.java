package testsupport;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.security.Principal;
import java.util.*;

/**
 * 极简版 HttpServletRequest stub（仅测试用）
 * - 通过构造参数传入请求 Body（字符串）
 * - 其它方法返回默认值或抛 UnsupportedOperationException
 */
public class StubHttpServletRequest implements HttpServletRequest {

    private final String body;
    private String characterEncoding = "UTF-8";
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String[]> params = new HashMap<>();
    private String method = "POST";
    private String contentType = "application/json";

    public StubHttpServletRequest(String body) {
        this.body = (body == null ? "" : body);
    }

    // ===== 常用：测试里会用到 =====
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(body.getBytes(getCharacterEncoding())), getCharacterEncoding())
        );
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        if (env != null) this.characterEncoding = env;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String ct) { this.contentType = ct; }

    public void setMethod(String m) { this.method = m; }

    @Override
    public String getMethod() { return method; }

    // ===== 下面都是为编译通过而实现的“默认/空实现” =====
    @Override public String getAuthType() { return null; }
    @Override public Cookie[] getCookies() { return new Cookie[0]; }
    @Override public long getDateHeader(String name) { return -1; }
    @Override public String getHeader(String name) { return headers.get(name); }
    @Override public Enumeration<String> getHeaders(String name) {
        String v = headers.get(name);
        return Collections.enumeration(v == null ? Collections.emptyList() : Collections.singletonList(v));
    }
    @Override public Enumeration<String> getHeaderNames() { return Collections.enumeration(headers.keySet()); }
    @Override public int getIntHeader(String name) { return -1; }
    @Override public String getPathInfo() { return null; }
    @Override public String getPathTranslated() { return null; }
    @Override public String getContextPath() { return ""; }
    @Override public String getQueryString() { return null; }
    @Override public String getRemoteUser() { return null; }
    @Override public boolean isUserInRole(String role) { return false; }
    @Override public Principal getUserPrincipal() { return null; }
    @Override public String getRequestedSessionId() { return null; }
    @Override public String getRequestURI() { return "/"; }
    @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost/"); }
    @Override public String getServletPath() { return ""; }
    @Override public HttpSession getSession(boolean create) { return null; }
    @Override public HttpSession getSession() { return null; }
    @Override public String changeSessionId() { return null; }
    @Override public boolean isRequestedSessionIdValid() { return false; }
    @Override public boolean isRequestedSessionIdFromCookie() { return false; }
    @Override public boolean isRequestedSessionIdFromURL() { return false; }
    @Override public boolean isRequestedSessionIdFromUrl() { return false; }

    @Override public boolean authenticate(HttpServletResponse response) { return false; }
    @Override public void login(String username, String password) {}
    @Override public void logout() {}
    @Override public Collection<Part> getParts() { return Collections.emptyList(); }
    @Override public Part getPart(String name) { return null; }
    @Override public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }

    @Override public Object getAttribute(String name) { return attributes.get(name); }
    @Override public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
    @Override public void setAttribute(String name, Object o) { attributes.put(name, o); }
    @Override public void removeAttribute(String name) { attributes.remove(name); }
    @Override public int getContentLength() { return body.getBytes().length; }
    @Override public long getContentLengthLong() { return body.getBytes().length; }

    @Override public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(body.getBytes(getCharacterEncoding()));
        return new ServletInputStream() {
            @Override public boolean isFinished() { return in.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) {}
            @Override public int read() { return in.read(); }
        };
    }

    @Override public String getParameter(String name) {
        String[] arr = params.get(name);
        return (arr == null || arr.length == 0) ? null : arr[0];
    }
    @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(params.keySet()); }
    @Override public String[] getParameterValues(String name) { return params.get(name); }
    @Override public Map<String, String[]> getParameterMap() { return params; }
    public void setParameter(String name, String value) { params.put(name, new String[]{value}); }

    @Override public String getProtocol() { return "HTTP/1.1"; }
    @Override public String getScheme() { return "http"; }
    @Override public String getServerName() { return "localhost"; }
    @Override public int getServerPort() { return 80; }
    public BufferedReader getReader(String charset) throws IOException { return getReader(); } // 兼容部分 IDE 补全
    @Override public String getRemoteAddr() { return "127.0.0.1"; }
    @Override public String getRemoteHost() { return "localhost"; }
    @Override public Locale getLocale() { return Locale.getDefault(); }
    @Override public Enumeration<Locale> getLocales() { return Collections.enumeration(Collections.singletonList(getLocale())); }
    @Override public boolean isSecure() { return false; }
    @Override public RequestDispatcher getRequestDispatcher(String path) { return null; }
    @Override public String getRealPath(String path) { return null; }
    @Override public int getRemotePort() { return 0; }
    @Override public String getLocalName() { return "localhost"; }
    @Override public String getLocalAddr() { return "127.0.0.1"; }
    @Override public int getLocalPort() { return 0; }
    @Override public ServletContext getServletContext() { return null; }
    @Override public AsyncContext startAsync() { return null; }
    @Override public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) { return null; }
    @Override public boolean isAsyncStarted() { return false; }
    @Override public boolean isAsyncSupported() { return false; }
    @Override public AsyncContext getAsyncContext() { return null; }
    @Override public DispatcherType getDispatcherType() { return DispatcherType.REQUEST; }
}
