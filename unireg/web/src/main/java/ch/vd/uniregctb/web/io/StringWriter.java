package ch.vd.uniregctb.web.io;

import org.apache.commons.lang.NullArgumentException;

public class StringWriter extends TextWriter {

    private final StringBuilder internalString;

    private boolean disposed = false;

    public StringWriter() {
        this(new StringBuilder());
    }

    public StringWriter(IFormatProvider formatProvider) {
        this(new StringBuilder(), formatProvider);
    }

    public StringWriter (StringBuilder sb)
{
        this (sb, null);
}

    public StringWriter(StringBuilder sb, IFormatProvider formatProvider) {
        if (sb == null)
            throw new NullArgumentException("sb");

        internalString = sb;
        internalFormatProvider = formatProvider;
    }

    @Override
    protected void finalize() throws Throwable {
        disposed = true;
        super.finalize();
    }

    public StringBuilder GetStringBuilder() {
        return internalString;
    }

    @Override
    public String toString() {
        return internalString.toString();
    }

    @Override
    public void write(char value) {
        if (disposed) {
            throw new RuntimeException("ObjectDisposedException");
        }

        internalString.append(value);
    }

    @Override
    public void write(String value) {
        if (disposed) {
            throw new RuntimeException("ObjectDisposedException");
        }

        internalString.append(value);
    }

    @Override
    public void write(char[] buffer, int index, int count) {
        if (disposed) {
            throw new RuntimeException("ObjectDisposedException");
        }
        if (buffer == null)
            throw new NullArgumentException("buffer");
        if (index < 0)
            throw new IndexOutOfBoundsException("index< 0");
        if (count < 0)
            throw new IndexOutOfBoundsException("count < 0");
        // re-ordered to avoid possible integer overflow
        if (index > buffer.length - count)
            throw new IllegalArgumentException("index + count > buffer.Length");

        internalString.append(buffer, index, count);
    }
}
