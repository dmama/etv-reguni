package ch.vd.uniregctb.inbox;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class InboxServiceTest extends WithoutSpringTest {

	private static final String CONTENT = "Lorem ipsum dolor sit amet";

	private InboxServiceImpl service;

	@Override
	public void onTearDown() throws Exception {
		if (service != null) {
			service.destroy();
		}
		super.onTearDown();
	}

	private void buildService(final long msCleaningPeriod) throws Exception {
		service = new InboxServiceImpl() {
			@Override
			protected long getCleaningPeriod() {
				return msCleaningPeriod;
			}
		};
		service.afterPropertiesSet();
	}

	private InputStream buildInputStream() {
		return new ByteArrayInputStream(CONTENT.getBytes());
	}

	@Test
	public void testDocumentReception() throws Exception {
		buildService(0);
		service.addDocument("LUI", "Impression...", null, "text/plain", buildInputStream(), 1);

		// on doit voir le document pour ce visa
		{
			final List<InboxElement> liste = service.getInboxContent("LUI");
			Assert.assertNotNull(liste);
			Assert.assertEquals(1, liste.size());

			final InboxElement elt = liste.get(0);
			Assert.assertEquals("Impression...", elt.getName());
		}

		// mais pas pour un autre visa
		{
			final List<InboxElement> liste = service.getInboxContent("ELLE");
			Assert.assertNotNull(liste);
			Assert.assertEquals(0, liste.size());
		}
	}

	@Test
	public void testDocumentExpiration() throws Exception {
		buildService(100);      // nettoyage toutes les 100 millisecondes
		final InboxElement element = new InboxElement("Mon impression qui va expirer", null, "text/plain", buildInputStream(), 500);
		service.addElement("MOI", element);

		Thread.sleep(300);

		// pour l'instant, le document est encore là
		{
			final List<InboxElement> liste = service.getInboxContent("MOI");
			Assert.assertNotNull(liste);
			Assert.assertEquals(1, liste.size());

			final InboxElement elt = liste.get(0);
			Assert.assertEquals("Mon impression qui va expirer", elt.getName());
		}

		Thread.sleep(400);

		// il devrait maintenant avoir disparu
		{
			final List<InboxElement> liste = service.getInboxContent("MOI");
			Assert.assertNotNull(liste);
			Assert.assertEquals(0, liste.size());
		}
	}
}
