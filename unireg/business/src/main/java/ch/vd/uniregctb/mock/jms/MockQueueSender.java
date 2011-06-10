package ch.vd.uniregctb.mock.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;

/**
 * Simple implementation de {@link QueueSender}.
 *
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2008/02/08 10:19:22 $)
 * @version $Revision: 1.2 $
 */
public final class MockQueueSender implements QueueSender {

	/**
	 *
	 */
	private final Queue queue;

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
	 *
	 * @param queue
	 *            queue destination
	 */
	public MockQueueSender(Queue queue) {
		this.queue = queue;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Queue getQueue() throws JMSException {
		return queue;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(Message amessage) throws JMSException {
		send(queue, amessage, deliveryMode, priority, timeToLive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(Queue aqueue, Message amessage) throws JMSException {
		send(aqueue, amessage, deliveryMode, priority, timeToLive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(Message amessage, int adeliveryMode, int apriority, long atimeToLive) throws JMSException {
		send(queue, amessage, deliveryMode, priority, timeToLive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(Queue aqueue, Message amessage, int adeliveryMode, int apriority, long atimeToLive) throws JMSException {
		if (aqueue instanceof MockQueue) {
			((MockQueue) aqueue).onMessage(amessage);
		}
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
	public int getDeliveryMode() throws JMSException {
		return deliveryMode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getDisableMessageID() throws JMSException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getDisableMessageTimestamp() throws JMSException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPriority() throws JMSException {
		return priority;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTimeToLive() throws JMSException {
		return timeToLive;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisableMessageID(boolean arg0) throws JMSException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDisableMessageTimestamp(boolean arg0) throws JMSException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPriority(int priority) throws JMSException {
		this.priority = priority;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTimeToLive(long timeToLive) throws JMSException {
		this.timeToLive = timeToLive;
	}

	@Override
	public Destination getDestination() throws JMSException {
		return queue;
	}

	@Override
	public void send(Destination queue, Message message) throws JMSException {
		if (queue instanceof MockQueue) {
			((MockQueue) queue).onMessage(message);
		}
	}

	@Override
	public void send(Destination queue, Message message, int arg2, int arg3, long arg4) throws JMSException {
		if (queue instanceof MockQueue) {
			((MockQueue) queue).onMessage(message);
		}
	}

}
