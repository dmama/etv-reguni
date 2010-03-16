/**
 *
 */
package ch.vd.uniregctb.evenement.externe.mock;

import java.io.ByteArrayOutputStream;

import org.apache.xmlbeans.XmlObject;

import ch.vd.uniregctb.evenement.externe.IEvenementExterne;
import ch.vd.uniregctb.evenement.externe.jms.EvenementExterneFacadeImpl;
import ch.vd.uniregctb.evenement.externe.jms.MessageListener;
import ch.vd.uniregctb.mock.jms.MockMessageListener;
import ch.vd.uniregctb.mock.jms.MockQueue;
import ch.vd.uniregctb.mock.jms.MockQueueReceiver;
import ch.vd.uniregctb.mock.jms.MockTextMessage;

/**
 * @author xcicfh
 *
 */
public class MockEvenementExterneFacade extends EvenementExterneFacadeImpl {

	private final MockQueue queue;
	private final MockQueueReceiver receiver;

	private int numberOfSend = 1;

	public MockEvenementExterneFacade() {
		super();
		queue = new MockQueue();
		receiver = new MockQueueReceiver(queue);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	@Override
	public void setMessageListener(MessageListener messageListener) {
		super.setMessageListener(messageListener);
		receiver.setMessageListener(new MockMessageListener(messageListener));
	}

	@Override
	public void destroy() throws Exception {
	}

	@Override
	public void sendEvent(IEvenementExterne evenementExterne) throws Exception {
		MockTextMessage message = new MockTextMessage(queue);
		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		writeXml(writer, (XmlObject) evenementExterne);
		message.setText(writer.toString());
		int count = numberOfSend;
		while ( count-- > 0) {
			receiver.sendMessage(message);
		}
	}

	/**
	 * @param numberOfSend the numberOfSend to set
	 */
	public void setNumberOfSend(int numberOfSend) {
		this.numberOfSend = numberOfSend;
	}

}
