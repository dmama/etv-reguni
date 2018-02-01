package ch.vd.unireg.jms;

/**
 * Classe de Mock pour le contexte Spring de test
 */
public class MockMessageListenerContainer implements MessageListenerContainerJmxInterface {

	@Override
	public String getDestinationName() {
		return "DummyDestination";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public int getMaxConcurrentConsumers() {
		return 0;
	}

	@Override
	public int getReceivedMessages() {
		return 0;
	}

	@Override
	public int getMessagesWithException() {
		return 0;
	}

	@Override
	public int getMessagesWithBusinessError() {
		return 0;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isRunning() {
		return false;
	}
}
