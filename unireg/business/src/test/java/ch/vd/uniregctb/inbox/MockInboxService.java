package ch.vd.uniregctb.inbox;

/**
 * Mock du service d'inbox, en particulier il n'a pas de thread de cleanup
 */
public class MockInboxService extends InboxServiceImpl {

	@Override
	protected long getCleaningPeriod() {
		return 0L;
	}
}
