package ch.vd.uniregctb.mock.jms;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueReceiver;

import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Simple implementation de {@link QueueReceiver}
 *
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:58:13 $)
 * @version $Revision: 1.1 $
 */
public final class MockQueueReceiver implements QueueReceiver, Reciever {

	/**
	 *
	 */
	private MessageListener messageListener;

	/**
	 *
	 */
	private Queue queue;

	/**
	 *
	 */
	private Message message;

	/**
	 * default constructor
	 */
	@SuppressWarnings("unused")
	private MockQueueReceiver() {

	}

	/**
	 *
	 * @param q
	 *            queue
	 */
	public MockQueueReceiver(Queue q) {
		this.queue = q;
		if (queue instanceof MockQueue) {
			((MockQueue) queue).addReciever(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendMessage(final Message amessage) throws JMSException {
		TaskExecutor task = new SyncTaskExecutor();
		task.execute(new Runnable() {
			@Override
			public void run() {
				messageListener.onMessage(amessage);
				try {
					if (amessage instanceof BytesMessage) {
						((BytesMessage) amessage).reset();
					}
				}
				catch (JMSException ex) {
					System.out.println(ex.getMessage());
					ex.printStackTrace();
				}
			}
		});

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
	public MessageListener getMessageListener() throws JMSException {
		return this.messageListener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
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
		}
		catch (Exception ex) {
			return null;
		}
		finally {
			message = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Message receiveNoWait() throws JMSException {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws JMSException {
		throw new UnsupportedOperationException();
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
	public void onRecieve(Message amessage) {
		this.message = amessage;
		if (this.messageListener != null) {
			messageListener.onMessage(message);
			this.message = null;
		}
	}
}
