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
    @Override
    public boolean getNoLocal() throws JMSException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Topic getTopic() throws JMSException {
        return topic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws JMSException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageListener getMessageListener() throws JMSException {
        return messageListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessageSelector() throws JMSException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message receive() throws JMSException {
        return receive(-1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public Message receiveNoWait() throws JMSException {
        return receive(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessageListener(MessageListener messageListener) throws JMSException {
        this.messageListener = messageListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRecieve(Message amessage) {
        this.message = amessage;
        if (this.messageListener != null) {
            messageListener.onMessage(message);
            this.message = null;
        }
    }

}
