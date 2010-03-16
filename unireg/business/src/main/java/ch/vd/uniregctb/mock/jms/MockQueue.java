package ch.vd.uniregctb.mock.jms;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

/**
 * Stub implementation of the {@link javax.jms.Queue} interface.
 *
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:58:13 $)
 * @version $Revision: 1.2 $
 */
public final class MockQueue implements Queue, Destination {

	/**
	 * default nom de la queue
	 */
	public static final String DEFAULT_QUEUE_NAME = "jms.InputQueue";

	/**
	 *
	 */
	private String queueName = DEFAULT_QUEUE_NAME;

	/**
	 *
	 */
	private final ArrayList<Reciever> receivers = new ArrayList<Reciever>();

	/**
	 * default constructor
	 *
	 */
	public MockQueue() {
	}

	/**
	 * default constructor
	 *
	 * @param queueName
	 *            nom de la queue
	 */
	public MockQueue(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getQueueName() {
		return this.queueName;
	}

	/**
	 * ajoute un recierver de message.
	 *
	 * @param reciever
	 *            un reciever.
	 */
	public void addReciever(Reciever reciever) {
		receivers.add(reciever);
	}

	/**
	 * appeller quand un message arrive
	 *
	 * @param message
	 *            un message JMS
	 */
	public void onMessage(Message message) {
		for (Iterator<Reciever> iter = receivers.iterator(); iter.hasNext();) {
			Reciever reciever = iter.next();
			reciever.onRecieve(message);
		}
	}

	@Override
	public String toString() {
		return queueName;
	}
}
