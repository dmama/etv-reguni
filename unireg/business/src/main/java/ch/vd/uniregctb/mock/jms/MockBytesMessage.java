package ch.vd.uniregctb.mock.jms;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import java.io.IOException;
import java.io.InputStream;

/**
 * messages implementation of the {@link javax.jms.BytesMessage} interface.
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2008/02/08 10:19:22 $)
 * @version $Revision: 1.2 $
 */
public final class MockBytesMessage extends MockMessage implements BytesMessage {

    /**
     *
     */
    private InputStream content;

    /**
     * default Contructor
     *
     */
    public MockBytesMessage() {
    }

    /**
     *
     * @param destination destionation
     * @param content contenu du message
     */
    public MockBytesMessage(Destination destination, InputStream content) {
        super(destination);
        this.content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void clearBody() throws JMSException {
        super.clearBody();
        this.content.mark(0);
        try {
            this.content.reset();
        } catch (IOException ex) {
            throw new JMSException(ex.getMessage());
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readBoolean() throws JMSException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte() throws JMSException {
        if (content == null) {
            return -1;
        }
        try {
            return (byte) content.read();
        } catch (IOException ex) {
            throw new JMSException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(byte[] buffer) throws JMSException {
        if (content == null) {
            return -1;
        }
        try {
            return content.read(buffer);
        } catch (IOException ex) {
            throw new JMSException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readBytes(byte[] buffer, int offset) throws JMSException {
        if (content == null) {
            return -1;
        }
        try {
            return content.read(buffer, offset, buffer.length);
        } catch (IOException ex) {
            throw new JMSException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readDouble() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float readFloat() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readUTF() throws JMSException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedByte() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readUnsignedShort() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws JMSException {
        super.clearProperties();
        try {
            if (content != null) {
                content.close();
            }
        } catch (IOException ex) {
            throw new JMSException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBoolean(boolean arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(byte arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(byte[] arg0, int arg1, int arg2) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(char arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(double arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(float arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(int arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(long arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(short arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTF(String arg0) throws JMSException {
    }



}
