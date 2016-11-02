package ch.vd.uniregctb.common;

import org.junit.Before;
import org.junit.Test;

import ch.vd.shared.batchtemplate.StatusManager;

import static org.junit.Assert.assertEquals;

public class SubStatusManagerTest {

	private MockStatusManager parent;

	@Before
	public void setUp() throws Exception {
		parent = new MockStatusManager();
	}

	@Test
	public void testSetMessage() throws Exception {
		final StatusManager sub = new SubStatusManager(0, 50,  parent);
		sub.setMessage("test");
		assertEquals("test", parent.getMsg());
	}

	@Test
	public void testRange_0_50() throws Exception {

		final StatusManager sub = new SubStatusManager(0, 50,  parent);

		sub.setMessage("zéro", 0);
		assertEquals(0, parent.getPercent());
		assertEquals("zéro", parent.getMsg());

		sub.setMessage("dix", 10);
		assertEquals(5, parent.getPercent());
		assertEquals("dix", parent.getMsg());

		sub.setMessage("cinquante", 50);
		assertEquals(25, parent.getPercent());
		assertEquals("cinquante", parent.getMsg());

		sub.setMessage("septante-cinq", 75);
		assertEquals(37, parent.getPercent());
		assertEquals("septante-cinq", parent.getMsg());

		sub.setMessage("cent", 100);
		assertEquals(50, parent.getPercent());
		assertEquals("cent", parent.getMsg());
	}

	@Test
	public void testRange_50_100() throws Exception {

		final StatusManager sub = new SubStatusManager(50, 100,  parent);

		sub.setMessage("zéro", 0);
		assertEquals(50, parent.getPercent());
		assertEquals("zéro", parent.getMsg());

		sub.setMessage("dix", 10);
		assertEquals(55, parent.getPercent());
		assertEquals("dix", parent.getMsg());

		sub.setMessage("cinquante", 50);
		assertEquals(75, parent.getPercent());
		assertEquals("cinquante", parent.getMsg());

		sub.setMessage("septante-cinq", 75);
		assertEquals(87, parent.getPercent());
		assertEquals("septante-cinq", parent.getMsg());

		sub.setMessage("cent", 100);
		assertEquals(100, parent.getPercent());
		assertEquals("cent", parent.getMsg());
	}
}