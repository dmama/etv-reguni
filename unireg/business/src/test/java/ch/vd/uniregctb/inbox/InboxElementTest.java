package ch.vd.uniregctb.inbox;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class InboxElementTest extends WithoutSpringTest {

	private static final String CONTENT = "Lorem ipsum dolor sit amet";

	private InputStream buildInputStream() {
		return new ByteArrayInputStream(CONTENT.getBytes());
	}

	@Test
	public void testStreamContent() throws Exception {
		final InputStream src = buildInputStream();
		final InboxElement elt = new InboxElement("Mon nom à moi", "Ma description", "text/plain", src, 100);
		Assert.assertFalse(elt.isExpired());
		Assert.assertEquals("Mon nom à moi", elt.getName());
		Assert.assertEquals("Ma description", elt.getDescription());
		Assert.assertEquals("text/plain", elt.getMimeType());
		Assert.assertNotNull(elt.getIncomingDate());
		Assert.assertFalse(elt.isRead());

		final byte[] bytes = new byte[CONTENT.length() + 10];
		final int read = elt.getContent().read(bytes);
		Assert.assertEquals(CONTENT.getBytes().length, read);
		Assert.assertEquals(CONTENT, new String(bytes, 0, read));
		Assert.assertTrue(elt.isRead());

		elt.onDiscard();
	}

	@Test
	public void testExpiration() throws Exception {
		final InputStream src = buildInputStream();
		final InboxElement elt = new InboxElement("Mon nom à moi", null, "text/plain", src, 100);
		Assert.assertFalse(elt.isExpired());
		Thread.sleep(200);
		Assert.assertTrue(elt.isExpired());

		elt.onDiscard();
	}

	@Test
	public void testNoExpiration() throws Exception {
		final InputStream src = buildInputStream();
		final InboxElement elt = new InboxElement("Mon nom à moi", null, "text/plain", src, 0);
		Assert.assertFalse(elt.isExpired());
		Thread.sleep(200);
		Assert.assertFalse(elt.isExpired());

		elt.onDiscard();
	}

	@Test
	public void testNaturalOrdering() throws Exception {
		final InputStream src1 = buildInputStream();
		final InboxElement elt1 = new InboxElement("Un", null, "text/plain", src1, 0);
		Thread.sleep(50);
		final InputStream src2 = buildInputStream();
		final InboxElement elt2 = new InboxElement("Deux", null, "text/plain", src2, 0);

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
	public void testNullInputStream() throws Exception {
		final InboxElement elt = new InboxElement("Rien", null, "text/csv", null, 0);
		Assert.assertNull(elt.getContent());
		elt.onDiscard();
	}

	@Test
	public void testUuidGeneration() throws Exception {
		final InboxElement eltSans = new InboxElement("Bidon", "Elément bidon", "text/plain", buildInputStream(), 0);
		Assert.assertNotNull(eltSans.getUuid());

		final UUID uuid = UUID.randomUUID();
		final InboxElement eltAvec = new InboxElement(uuid, "Bidon 2", "Deuxième élément bidon", "text/plain", buildInputStream(), 0);
		Assert.assertEquals(uuid, eltAvec.getUuid());
	}
}
