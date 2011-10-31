package ch.vd.uniregctb.inbox;

import java.util.List;

import org.apache.commons.lang.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class InboxContainerTest extends WithoutSpringTest {

	private InboxContainer container;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		container = new InboxContainer();
	}

	@Override
	public void onTearDown() throws Exception {
		container.cleanup(false);
		super.onTearDown();
	}

	@Test
	public void testSimpleAjoutEtRecuperation() throws Exception {
		final InboxElement elt = new InboxElement("Bidon", "Ceci est bidon", null, 0);
		container.addElement("MOI", elt);

		Assert.assertEquals(elt, container.get(elt.getUuid()));
		Assert.assertEquals(elt, container.getInboxContent("MOI").get(0));
	}

	@Test
	public void testDocumentOrdering() throws Exception {
		final InboxElement un = new InboxElement("UN", null, null, 0);
		container.addElement("MOI", un);

		Thread.sleep(100);

		final InboxElement deux = new InboxElement("DEUX", null, null, 0);
		container.addElement("MOI", deux);

		// dans quel ordre sont-il rendus ?
		final List<InboxElement> liste = container.getInboxContent("MOI");
		Assert.assertNotNull(liste);
		Assert.assertEquals(2, liste.size());
		Assert.assertEquals("DEUX", liste.get(0).getName());
		Assert.assertEquals("UN", liste.get(1).getName());
	}

	@Test
	public void testNotificationNewInbox() throws Exception {
		final MutableInt nbNotifications = new MutableInt(0);
		final InboxManagementListener listener = new InboxManagementListener() {
			@Override
			public void onNewInbox(String visa) {
				nbNotifications.increment();
			}
		};
		Assert.assertEquals(0, nbNotifications.intValue());
		container.registerInboxManagementListener(listener, true);
		Assert.assertEquals(0, nbNotifications.intValue());
		container.addElement("TOTO", new InboxElement("test", "Message de test", null, 0));
		Assert.assertEquals(1, nbNotifications.intValue());
		container.addElement("TOTO", new InboxElement("test2", "Message de test", null, 0));
		Assert.assertEquals(1, nbNotifications.intValue());
	}

	@Test
	public void testNotificationNewInboxWithCatchup() throws Exception {
		final MutableInt nbNotifications = new MutableInt(0);
		final InboxManagementListener listener = new InboxManagementListener() {
			@Override
			public void onNewInbox(String visa) {
				nbNotifications.increment();
			}
		};
		container.addElement("TOTO", new InboxElement("test", "Message de test", null, 0));
		Assert.assertEquals(0, nbNotifications.intValue());
		container.registerInboxManagementListener(listener, true);
		Assert.assertEquals(1, nbNotifications.intValue());
		container.addElement("TOTO", new InboxElement("test2", "Message de test", null, 0));
		Assert.assertEquals(1, nbNotifications.intValue());
		container.addElement("TATA", new InboxElement("test3", "Message de test", null, 0));
		Assert.assertEquals(2, nbNotifications.intValue());
	}

	@Test
	public void testNotificationNewInboxWithoutCatchup() throws Exception {
		final MutableInt nbNotifications = new MutableInt(0);
		final InboxManagementListener listener = new InboxManagementListener() {
			@Override
			public void onNewInbox(String visa) {
				nbNotifications.increment();
			}
		};
		container.addElement("TOTO", new InboxElement("test", "Message de test", null, 0));
		Assert.assertEquals(0, nbNotifications.intValue());
		container.registerInboxManagementListener(listener, false);
		Assert.assertEquals(0, nbNotifications.intValue());
		container.addElement("TOTO", new InboxElement("test2", "Message de test", null, 0));
		Assert.assertEquals(0, nbNotifications.intValue());     // normal, il ne s'agit pas d'une nouvelle inbox
		container.addElement("TATA", new InboxElement("test3", "Message de test", null, 0));
		Assert.assertEquals(1, nbNotifications.intValue());
	}

	@Test
	public void testNotificationListenerApresExplosion() throws Exception {
		final MutableInt nbNotifications = new MutableInt(0);
		final InboxManagementListener listener = new InboxManagementListener() {
			@Override
			public void onNewInbox(String visa) {
				nbNotifications.increment();
			}
		};
		final InboxManagementListener boom = new InboxManagementListener() {
			@Override
			public void onNewInbox(String visa) {
				throw new IllegalArgumentException("Exception de test!");
			}
		};
		container.registerInboxManagementListener(boom, false);
		container.registerInboxManagementListener(listener, false);
		Assert.assertEquals(0, nbNotifications.intValue());
		container.addElement("TOTO", new InboxElement("test", "Message de test", null, 0));
		Assert.assertEquals(1, nbNotifications.intValue());
	}
}
