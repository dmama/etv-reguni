package ch.vd.uniregctb.web.xt.component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public final class InternalHttpServletResponse implements HttpServletResponse {

    private final StringWriter writer;
    private final ServletOutputStream out;

    private String characterEncoding;
    private String contentType;
    private Locale locale;

    public InternalHttpServletResponse(StringWriter writer) {
        this.writer = writer;
        this.out = new ServletOutputStream() {
            @Override
			public void write(int b) throws IOException {
                InternalHttpServletResponse.this.getWriter().append((char) b);
            }
        };
    }

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void addDateHeader(String name, long date) {

    }

    @Override
    public void addHeader(String name, String value) {

    }

    @Override
    public void addIntHeader(String name, int value) {

    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return url;
    }

    @Override
    public String encodeURL(String url) {
        return url;
    }
    @Override
    public String encodeUrl(String url) {
        return url;
    }

    @Override
    public void sendError(int sc) throws IOException {

    }

    @Override
    public void sendError(int sc, String msg) throws IOException {

    }

    @Override
    public void sendRedirect(String location) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDateHeader(String name, long date) {

    }

    @Override
    public void setHeader(String name, String value) {

    }

    @Override
    public void setIntHeader(String name, int value) {

    }

    @Override
    public void setStatus(int sc) {

    }

    @Override
    public void setStatus(int sc, String sm) {

    }

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public int getBufferSize() {
        return this.writer.getBuffer().length();
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.out;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(this.writer);
    }

    @Override
    public boolean isCommitted() {
    	return true;
    }

    @Override
    public void reset() {
        writer.getBuffer().setLength(0);
    }

    @Override
    public void resetBuffer() {
        this.reset();
    }

    @Override
    public void setBufferSize(int size) {
    	setContentLength(size);
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public void setContentLength(int len) {
    	writer.getBuffer().setLength(len);
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
    }

    @Override
    public void setLocale(Locale loc) {
        this.locale = loc;
    }
}