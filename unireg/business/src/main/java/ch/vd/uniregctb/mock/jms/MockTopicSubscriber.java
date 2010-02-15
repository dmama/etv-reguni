package ch.vd.uniregctb.mock.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * Simple impelementation de {@link TopicSubscriber}
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:58:13 $)
 * @version $Revision: 1.1 $
 */
public final class MockTopicSubscriber implements TopicSubscriber , Reciever {

    /**
     *
     */
    private MessageListener messageListener;

    /**
     *
     */
    private Message message;

    /**
     *
     */
    private final Topic topic;

    /**
     * Default constructor
     * @param topic topic associé.
     */
    public MockTopicSubscriber(Topic topic) {
        this.topic = topic;
        if (topic instanceof MockTopic) {
           ((MockTopic) topic).addReciever(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getNoLocal() throws JMSException {
        return false;
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
    public void close() throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    public MessageListener getMessageListener() throws JMSException {
        return messageListener;
    }

    /**
     * {@inheritDoc}
     */
    public String getMessageSelector() throws JMSException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Message receive() throws JMSException {
        return receive(-1);
    }

    /**
     * {@inheritDoc}
     */
    public Message receive(long timeout) throws JMSException {
        try {
            long lastTime = System.currentTimeMillis();
            long timeoutCvt = timeout;
            if (timeout == -1) {
                timeoutCvt = Long.MAX_VALUE;
            }
            while (((System.currentTimeMillis() - lastTime) < timeoutCvt) && (message == null)) {
                Thread.sleep(300);
            }
            return message;
        } catch (Exception ex) {
            return null;
        } finally {
            message = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Message receiveNoWait() throws JMSException {
        return receive(0);
    }

    /**
     * {@inheritDoc}
     */
    public void setMessageListener(MessageListener messageListener) throws JMSException {
        this.messageListener = messageListener;
    }

    /**
     * {@inheritDoc}
     */
    public void onRecieve(Message amessage) {
        this.message = amessage;
        if (this.messageListener != null) {
            messageListener.onMessage(message);
            this.message = null;
        }
    }

}
