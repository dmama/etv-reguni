package ch.vd.watchdog.editique;

import java.util.ArrayList;
import java.util.List;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageListener;

public class ListenerImpl extends EsbMessageListener implements Listener {

	private List<EsbMessage> receivedMessages = new ArrayList<EsbMessage>();

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		receivedMessages.add(message);
	}

	@Override
	public List<EsbMessage> getReceivedMessages() {
		return receivedMessages;
	}

	@Override
	public int receivedCount() {
		return receivedMessages.size();
	}

	@Override
	public void clear() {
		receivedMessages.clear();
	}
}
