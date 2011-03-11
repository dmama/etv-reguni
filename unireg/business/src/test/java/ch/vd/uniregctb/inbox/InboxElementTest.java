package ch.vd.uniregctb.inbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class InboxElementTest extends WithoutSpringTest {

	private static final String CONTENT = "Lorem ipsum dolor sit amet";

	private InboxAttachment buildAttachment() throws IOException {
		return new InboxAttachment("text/plain", new ByteArrayInputStream(CONTENT.getBytes()), "doloris.txt");
	}

	@Test
	public void testStreamContent() throws Exception {
		final InboxElement elt = new InboxElement("Mon nom à moi", "Ma description", buildAttachment(), 100);
		Assert.assertFalse(elt.isExpired());
		Assert.assertEquals("Mon nom à moi", elt.getName());
		Assert.assertEquals("Ma description", elt.getDescription());

		final InboxAttachment attachment = elt.getAttachment();
		Assert.assertNotNull(attachment);
		Assert.assertEquals("text/plain", attachment.getMimeType());
		Assert.assertEquals("doloris.txt", attachment.getFilename());

		Assert.assertNotNull(elt.getIncomingDate());
		Assert.assertFalse(elt.isRead());

		final byte[] bytes = new byte[CONTENT.length() + 10];
		final int read = attachment.getContent().read(bytes);
		Assert.assertEquals(CONTENT.getBytes().length, read);
		Assert.assertEquals(CONTENT, new String(bytes, 0, read));
		Assert.assertFalse(elt.isRead());

		elt.setRead(true);
		Assert.assertTrue(elt.isRead());

		elt.onDiscard();
	}

	@Test
	public void testExpiration() throws Exception {
		final InboxElement elt = new InboxElement("Mon nom à moi", null, buildAttachment(), 100);
		Assert.assertFalse(elt.isExpired());
		Thread.sleep(200);
		Assert.assertTrue(elt.isExpired());

		elt.onDiscard();
	}

	@Test
	public void testNoExpiration() throws Exception {
		final InboxElement elt = new InboxElement("Mon nom à moi", null, buildAttachment(), 0);
		Assert.assertFalse(elt.isExpired());
		Thread.sleep(200);
		Assert.assertFalse(elt.isExpired());

		elt.onDiscard();
	}

	@Test
	public void testNaturalOrdering() throws Exception {
		final InboxElement elt1 = new InboxElement("Un", null, buildAttachment(), 0);
		Thread.sleep(50);
		final InboxElement elt2 = new InboxElement("Deux", null, buildAttachment(), 0);

		final List<InboxElement> list = new ArrayList<InboxElement>(2);
		list.add(elt1);
		list.add(elt2);
		Collections.sort(list);
		Assert.assertEquals(elt2, list.get(0));
		Assert.assertEquals(elt1, list.get(1));

		elt1.onDiscard();
		elt2.onDiscard();
	}

	@Test
	public void testSansAttachement() throws Exception {
		final InboxElement elt = new InboxElement("Rien", null, null, 0);
		Assert.assertNull(elt.getAttachment());
		elt.onDiscard();
	}

	@Test
	public void testUuidGeneration() throws Exception {
		final InboxElement eltSans = new InboxElement("Bidon", "Elément bidon", buildAttachment(), 0);
		Assert.assertNotNull(eltSans.getUuid());
		eltSans.onDiscard();

		final UUID uuid = UUID.randomUUID();
		final InboxElement eltAvec = new InboxElement(uuid, "Bidon 2", "Deuxième élément bidon", buildAttachment(), 0);
		Assert.assertEquals(uuid, eltAvec.getUuid());
		eltAvec.onDiscard();
	}
}
