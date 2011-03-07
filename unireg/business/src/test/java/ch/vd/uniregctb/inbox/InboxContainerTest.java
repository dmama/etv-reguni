package ch.vd.uniregctb.inbox;

import java.util.List;

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
		final InboxElement elt = new InboxElement("Bidon", "Ceci est bidon", null, null, 0);
		container.addElement("MOI", elt);

		Assert.assertEquals(elt, container.get(elt.getUuid()));
		Assert.assertEquals(elt, container.getInboxContent("MOI").get(0));
	}

	@Test
	public void testDocumentOrdering() throws Exception {
		final InboxElement un = new InboxElement("UN", null, null, null, 0);
		container.addElement("MOI", un);

		Thread.sleep(100);

		final InboxElement deux = new InboxElement("DEUX", null, null, null, 0);
		container.addElement("MOI", deux);

		// dans quel ordre sont-il rendus ?
		final List<InboxElement> liste = container.getInboxContent("MOI");
		Assert.assertNotNull(liste);
		Assert.assertEquals(2, liste.size());
		Assert.assertEquals("DEUX", liste.get(0).getName());
		Assert.assertEquals("UN", liste.get(1).getName());
	}
}
