package ch.vd.uniregctb.mock.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.jms.listener.SessionAwareMessageListener;

public class MockMessageListener implements MessageListener {

	private final Object messageListener;

	public MockMessageListener(Object messageListener) {
		this.messageListener = messageListener;
	}

	@Override
	public void onMessage(Message message) {
		if ( messageListener instanceof MessageListener){
			((MessageListener)messageListener).onMessage(message);
		} else if ( messageListener instanceof SessionAwareMessageListener) {
			try {
				((SessionAwareMessageListener)messageListener).onMessage(message, null);
			}
			catch (JMSException e) {
				throw new RuntimeException(e);
			}
		}
	}


}
