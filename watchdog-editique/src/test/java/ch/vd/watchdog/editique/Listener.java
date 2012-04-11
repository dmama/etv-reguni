package ch.vd.watchdog.editique;

import java.util.List;

import ch.vd.technical.esb.EsbMessage;

public interface Listener {
	List<EsbMessage> getReceivedMessages();

	int receivedCount();

	void clear();
}
