package ch.vd.uniregctb.mock.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicPublisher;

/**
 * Simple implementation de {@link TopicPublisher}.
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2008/02/08 10:19:22 $)
 * @version $Revision: 1.2 $
 */
public final class MockTopicPublisher implements TopicPublisher {

    /**
     *
     */
    private final Topic topic;

    /**
     *
     */
    private int deliveryMode;

    /**
     *
     */
    private int priority;

    /**
     *
     */
    private long timeToLive;

    /**
     * Default constructor
     * @param topic un topic
     */
    public MockTopicPublisher(Topic topic) {
        this.topic = topic;
    }

    /**
     * {@inheritDoc}
     */
    public Topic getTopic() throws JMSException {
        return topic;
    }

    /**
     * {@inheritDoc}
     */
    public void publish(Message amessage) throws JMSException {
        publish(this.topic, amessage, this.deliveryMode, this.priority, this.timeToLive);
    }

    /**
     * {@inheritDoc}
     */
    public void publish(Topic atopic, Message amessage) throws JMSException {
        publish(atopic, amessage, this.deliveryMode, this.priority, this.timeToLive);
    }

    /**
     * {@inheritDoc}
     */
    public void publish(Message message, int adeliveryMode, int apriority, long atimeToLive) throws JMSException {
        publish(this.topic, message, adeliveryMode, apriority, atimeToLive);
    }

    /**
     * {@inheritDoc}
     */
    public void publish(Topic atopic, Message amessage, int adeliveryMode, int apriority, long atimeToLive) throws JMSException {
        if (atopic instanceof MockTopic) {
            ((MockTopic) topic).onMessage(amessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws JMSException {

    }

    /**
     * {@inheritDoc}
     */
    public int getDeliveryMode() throws JMSException {
        return deliveryMode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getDisableMessageID() throws JMSException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getDisableMessageTimestamp() throws JMSException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() throws JMSException {
        return priority;
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeToLive() throws JMSException {
        return timeToLive;
    }

    /**
     * {@inheritDoc}
     */
    public void setDeliveryMode(int deliveryMode) throws JMSException {
       this.deliveryMode = deliveryMode;
    }

    /**
     * {@inheritDoc}
     */
    public void setDisableMessageID(boolean arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void setDisableMessageTimestamp(boolean arg0) throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public void setPriority(int priority) throws JMSException {
        this.priority = priority;
    }

    /**
     * {@inheritDoc}
     */
    public void setTimeToLive(long timeToLive) throws JMSException {
        this.timeToLive = timeToLive;
    }

    public Destination getDestination() throws JMSException {
        return null;
    }

    public void send(Message arg0) throws JMSException {
    }

    public void send(Destination arg0, Message arg1) throws JMSException {
    }

    public void send(Message arg0, int arg1, int arg2, long arg3) throws JMSException {
    }

    public void send(Destination arg0, Message arg1, int arg2, int arg3, long arg4) throws JMSException {
    }

}
