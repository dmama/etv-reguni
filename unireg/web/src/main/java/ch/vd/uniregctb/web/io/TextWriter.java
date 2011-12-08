package ch.vd.uniregctb.web.io;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.SystemUtils;

public abstract class TextWriter {

    public static final TextWriter Null = new NullTextWriter();

    protected String coreNewLine;

    protected IFormatProvider internalFormatProvider;

    protected TextWriter() {
        coreNewLine = SystemUtils.LINE_SEPARATOR;
    }

    protected TextWriter(IFormatProvider formatProvider) {
        coreNewLine = SystemUtils.LINE_SEPARATOR;
        internalFormatProvider = formatProvider;
    }

    // public abstract Encoding Encoding { get; }

    public IFormatProvider getFormatProvider() {
        return internalFormatProvider;
    }

    public String getNewLine() {
        return coreNewLine;
    }

    public void setNewLine(String value) {
        if (value == null) {
            value = SystemUtils.LINE_SEPARATOR;
        }

        coreNewLine = value;
    }

    public void close() {

    }

    public void flush() {
        // do nothing
    }

    public static TextWriter Synchronized(TextWriter writer) {
        return Synchronized(writer, false);
    }

    static TextWriter Synchronized(TextWriter writer, boolean neverClose) {
        if (writer == null)
            throw new NullArgumentException("writer is null");

        if (writer instanceof SynchronizedWriter)
            return writer;

        return new SynchronizedWriter(writer, neverClose);
    }

    public void write(boolean value) {
        write(Boolean.toString(value));
    }

    public void write(char value) {
        // Do nothing
    }

    public void write(char[] value) {
        if (value == null)
            return;
        write(value, 0, value.length);
    }

    public void write(double value) {
        write(Double.toString(value));
    }

    public void write(int value) {
        write(Integer.toString(value));
    }

    public void write(long value) {
        write(Long.toString(value));
    }

    public void write(Object value) {
        if (value != null)
            write(value.toString());
    }

    public void write(float value) {
        write(Float.toString(value));
    }

    public void write(String value) {
        if (value != null)
            write(value.toCharArray());
    }

    public void write(String format, Object... args) {
        write(String.format(format, args));
    }

    public void write(String format, Object arg0) {
        write(String.format(format, arg0));
    }

    public void write(char[] buffer, int index, int count) {
        if (buffer == null)
            throw new NullArgumentException("buffer");
        if (index < 0 || index > buffer.length)
            throw new IndexOutOfBoundsException("index");
        // re-ordered to avoid possible integer overflow
        if (count < 0 || (index > buffer.length - count))
            throw new IndexOutOfBoundsException("count");

        for (; count > 0; --count, ++index) {
            write(buffer[index]);
        }
    }

    public void write(String format, Object arg0, Object arg1) {
        write(String.format(format, arg0, arg1));
    }

    public void write(String format, Object arg0, Object arg1, Object arg2) {
        write(String.format(format, arg0, arg1, arg2));
    }

    public void writeLine() {
        write(coreNewLine);
    }

    public void writeLine(boolean value) {
        write(value);
        writeLine();
    }

    public void writeLine(char value) {
        write(value);
        writeLine();
    }

    public void writeLine(char[] value) {
        write(value);
        writeLine();
    }

    public void writeLine(double value) {
        write(value);
        writeLine();
    }

    public void writeLine(int value) {
        write(value);
        writeLine();
    }

    public void writeLine(long value) {
        write(value);
        writeLine();
    }

    public void writeLine(Object value) {
        write(value);
        writeLine();
    }

    public void writeLine(float value) {
        write(value);
        writeLine();
    }

    public void writeLine(String value) {
        write(value);
        writeLine();
    }

    public void writeLine(String format, Object arg0) {
        write(format, arg0);
        writeLine();
    }

    public void writeLine(String format, Object... arg) {
        write(format, arg);
        writeLine();
    }

    public void writeLine(char[] buffer, int index, int count) {
        write(buffer, index, count);
        writeLine();
    }

    public void writeLine(String format, Object arg0, Object arg1) {
        write(format, arg0, arg1);
        writeLine();
    }

    public void writeLine(String format, Object arg0, Object arg1, Object arg2) {
        write(format, arg0, arg1, arg2);
        writeLine();
    }

    //
    // Null version of the TextWriter, for the `Null' instance variable
    //
    static class NullTextWriter extends TextWriter {

        @Override
        public void write(String s) {
        }

        @Override
        public void write(char value) {
        }

        @Override
        public void write(char[] value, int index, int count) {
        }
    }
}

//
// Sychronized version of the TextWriter.
//
class SynchronizedWriter extends TextWriter {
    private final TextWriter writer;

    private final boolean neverClose;

    public SynchronizedWriter(TextWriter writer) {
        this(writer, false);

    }

    public SynchronizedWriter(TextWriter writer, boolean neverClose) {
        this.writer = writer;
        this.neverClose = neverClose;
    }

    @Override
    public void close() {
        if (neverClose)
            return;
        synchronized (this) {
            writer.close();
        }
    }

    @Override
    public void flush() {
        synchronized (this) {
            writer.flush();
        }
    }

    @Override
    public void write(boolean value) {
        synchronized (this) {
            writer.write(value);
        }
    }

    @Override
    public void write(char value) {
        synchronized (this) {
            writer.write(value);
        }
    }

    @Override
    public void write(char[] value) {
        synchronized (this) {
            writer.write(value);
        }
    }


    @Override
    public void write(int value) {
        synchronized (this) {
            writer.write(value);
        }
    }

    @Override
    public void write(long value) {
        synchronized (this) {
            writer.write(value);
        }
    }

    @Override
    public void write(Object value) {
        synchronized (this) {
            writer.write(value);
        }
    }

    @Override
    public void write(float value) {
        synchronized (this) {
            writer.write(value);
        }
    }

    @Override
    public void write(String value) {
        synchronized (this) {
            writer.write(value);
        }
    }



    @Override
    public void write(String format, Object value) {
        synchronized (this) {
            writer.write(format, value);
        }
    }

    @Override
    public void write(String format, Object... value) {
        synchronized (this) {
            writer.write(format, value);
        }
    }

    @Override
    public void write(char[] buffer, int index, int count) {
        synchronized (this) {
            writer.write(buffer, index, count);
        }
    }

    @Override
    public void write(String format, Object arg0, Object arg1) {
        synchronized (this) {
            writer.write(format, arg0, arg1);
        }
    }

    @Override
    public void write(String format, Object arg0, Object arg1, Object arg2) {
        synchronized (this) {
            writer.write(format, arg0, arg1, arg2);
        }
    }

    @Override
    public void writeLine() {
        synchronized (this) {
            writer.writeLine();
        }
    }

    @Override
    public void writeLine(boolean value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(char value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(char[] value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(double value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(int value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(long value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(Object value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(float value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }

    @Override
    public void writeLine(String value) {
        synchronized (this) {
            writer.writeLine(value);
        }
    }


    @Override
    public void writeLine(String format, Object value) {
        synchronized (this) {
            writer.writeLine(format, value);
        }
    }

    @Override
    public void writeLine(String format, Object... value) {
        synchronized (this) {
            writer.writeLine(format, value);
        }
    }

    @Override
    public void writeLine(char[] buffer, int index, int count) {
        synchronized (this) {
            writer.writeLine(buffer, index, count);
        }
    }

    @Override
    public void writeLine(String format, Object arg0, Object arg1) {
        synchronized (this) {
            writer.writeLine(format, arg0, arg1);
        }
    }

    public void WriteLine(String format, Object arg0, Object arg1, Object arg2) {
        synchronized (this) {
            writer.writeLine(format, arg0, arg1, arg2);
        }
    }


    @Override
    public IFormatProvider getFormatProvider() {
        synchronized (this) {
            return writer.getFormatProvider();
        }
    }

    @Override
    public String getNewLine() {
        synchronized (this) {
            return writer.getNewLine();
        }
    }

    @Override
    public void setNewLine(String value) {
        synchronized (this) {
            writer.setNewLine( value);
        }
    }

}