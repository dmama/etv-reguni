package ch.vd.uniregctb.mock.jms;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;

/**
 * essages implementation of the {@link javax.jms.Message} interface.
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2008/02/08 10:19:22 $)
 * @version $Revision: 1.2 $
 */
public class MockMessage implements Message {

    /**
     *
     */
    private Destination destination;

    /**
     *
     */
    private Destination destinationReply;

    /**
     *
     */
    private String jmsCorrelationID;

    private final long timestamp;

    /**
     * default Contructor
     *
     */
    public MockMessage() {
    	timestamp = System.currentTimeMillis();
    }


    /**
     * Default constructor
     * @param destination une destionation
     */
    public MockMessage(Destination destination) {
    	this();
        this.destination = destination;
    }

    /**
     *
     */
    private final Map<String,Object> properties = new HashMap<String, Object>();

    /**
     * {@inheritDoc}
     */
    public void acknowledge() throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void clearBody() throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public final void clearProperties() throws JMSException {
        properties.clear();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean getBooleanProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj == null) {
            throw new JMSException("key doesn't exist.");
        }
        try {
            return (Boolean) properties.get(key);
        } catch (Exception ex) {
            throw new MessageFormatException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final byte getByteProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj == null) {
            throw new JMSException("key doesn't exist.");
        }
        try {
            return (Byte) properties.get(key);
        } catch (Exception ex) {
            throw new MessageFormatException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final double getDoubleProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj == null) {
            throw new JMSException("key doesn't exist.");
        }
        try {
            return (Double) properties.get(key);
        } catch (Exception ex) {
            throw new MessageFormatException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final float getFloatProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj == null) {
            throw new JMSException("key doesn't exist.");
        }
        try {
            return ((Double) properties.get(key)).floatValue();
        } catch (Exception ex) {
            throw new MessageFormatException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final int getIntProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj == null) {
            throw new JMSException("key doesn't exist.");
        }
        try {
            return (Integer) properties.get(key);
        } catch (Exception ex) {
            throw new MessageFormatException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final String getJMSCorrelationID() throws JMSException {
        return jmsCorrelationID;
    }

    /**
     * {@inheritDoc}
     */
    public final byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return jmsCorrelationID.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public final int getJMSDeliveryMode() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public final Destination getJMSDestination() throws JMSException {
        return destination;
    }

    /**
     * {@inheritDoc}
     */
    public final long getJMSExpiration() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public final String getJMSMessageID() throws JMSException {
        return this.destination.toString() + "-" +this.getJMSTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    public final int getJMSPriority() throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean getJMSRedelivered() throws JMSException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public final Destination getJMSReplyTo() throws JMSException {
        return this.destinationReply;
    }

    /**
     * {@inheritDoc}
     */
    public final long getJMSTimestamp() throws JMSException {
        return timestamp;
    }

    /**
     * {@inheritDoc}
     */
    public final String getJMSType() throws JMSException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public final long getLongProperty(String key) throws JMSException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public final Object getObjectProperty(String key) throws JMSException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public final Enumeration<String> getPropertyNames() throws JMSException {
        return java.util.Collections.enumeration(properties.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public final short getShortProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj != null) {
            return (Short) obj;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public final String getStringProperty(String key) throws JMSException {
        Object obj = properties.get(key);
        if (obj != null) {
            return obj.toString();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean propertyExists(String key) throws JMSException {
        return properties.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public final void setBooleanProperty(String key, boolean value)
            throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setByteProperty(String key, byte value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setDoubleProperty(String key, double value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setFloatProperty(String key, float value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setIntProperty(String key, int value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setJMSCorrelationID(String correlationID) throws JMSException {
        this.jmsCorrelationID = correlationID;
    }

    /**
     * {@inheritDoc}
     */
    public final void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
        this.jmsCorrelationID = new String(correlationID);
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSDeliveryMode(int arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public final void setJMSDestination(Destination dest) throws JMSException {
        this.destination = dest;
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSExpiration(long expiration) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSMessageID(String messageID) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSPriority(int priority) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSRedelivered(boolean redelivered) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public final void setJMSReplyTo(Destination dest) throws JMSException {
        this.destinationReply = dest;
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSTimestamp(long timestamp) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void setJMSType(String type) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public final void setLongProperty(String key, long value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setObjectProperty(String key, Object value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setShortProperty(String key, short value) throws JMSException {
        properties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final void setStringProperty(String key, String value) throws JMSException {
        properties.put(key, value);
    }

    public long getBodyLength() throws JMSException {
        return 0;
    }
}
