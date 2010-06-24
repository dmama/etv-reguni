package ch.vd.uniregctb.web.xt.component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

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

    public void addCookie(Cookie cookie) {
    }

    public void addDateHeader(String name, long date) {

    }

    public void addHeader(String name, String value) {

    }

    public void addIntHeader(String name, int value) {

    }

    public boolean containsHeader(String name) {
        return false;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeRedirectUrl(String url) {
        return url;
    }

    public String encodeURL(String url) {
        return url;
    }
    public String encodeUrl(String url) {
        return url;
    }

    public void sendError(int sc) throws IOException {

    }

    public void sendError(int sc, String msg) throws IOException {

    }

    public void sendRedirect(String location) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void setDateHeader(String name, long date) {

    }

    public void setHeader(String name, String value) {

    }

    public void setIntHeader(String name, int value) {

    }

    public void setStatus(int sc) {

    }

    public void setStatus(int sc, String sm) {

    }

    public void flushBuffer() throws IOException {
    }

    public int getBufferSize() {
        return this.writer.getBuffer().length();
    }

    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return this.out;
    }

    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(this.writer);
    }

    public boolean isCommitted() {
    	return true;
    }

    public void reset() {
        writer.getBuffer().setLength(0);
    }

    public void resetBuffer() {
        this.reset();
    }

    public void setBufferSize(int size) {
    	setContentLength(size);
    }

    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    public void setContentLength(int len) {
    	writer.getBuffer().setLength(len);
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public void setLocale(Locale loc) {
        this.locale = loc;
    }
}