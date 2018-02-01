package ch.vd.unireg.registrefoncier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdentifiantAffaireRFTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("006-2006/1402/0", new IdentifiantAffaireRF(6, 2006, 1402, 0).toString());
		assertEquals("006", new IdentifiantAffaireRF(6, null, null, null).toString());
		assertEquals("006-3783", new IdentifiantAffaireRF(6, null, 3783, null).toString());
		assertEquals("001-1998/3535", new IdentifiantAffaireRF(1, 1998, 3535, null).toString());
	}

	@Test
	public void testParse() throws Exception {

		assertEquals(new IdentifiantAffaireRF(6, 2006, 1402, 0), IdentifiantAffaireRF.parse("006-2006/1402/0"));
		assertEquals(new IdentifiantAffaireRF(6, "191-1"), IdentifiantAffaireRF.parse("006-191-1"));
		assertEquals(new IdentifiantAffaireRF(2, "84538"), IdentifiantAffaireRF.parse("002-84538"));
		assertEquals(new IdentifiantAffaireRF(2, "115'038"), IdentifiantAffaireRF.parse("002-115'038"));
		assertEquals(new IdentifiantAffaireRF(6, "03/409bis"), IdentifiantAffaireRF.parse("006-03/409bis"));
		assertEquals(new IdentifiantAffaireRF(5, "2002/341c"), IdentifiantAffaireRF.parse("005-2002/341c"));
		assertEquals(new IdentifiantAffaireRF(6, null, null, null), IdentifiantAffaireRF.parse("006"));
		assertEquals(new IdentifiantAffaireRF(1, 1998, 3535, null), IdentifiantAffaireRF.parse("001-1998/3535"));

		try {
			IdentifiantAffaireRF.parse("6-2006/1402/0");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [6-2006/1402/0] ne représente pas un identifiant d'affaire RF valide", e.getMessage());
		}

		try {
			IdentifiantAffaireRF.parse("01-1998");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [01-1998] ne représente pas un identifiant d'affaire RF valide", e.getMessage());
		}
	}

	@Test
	public void testCompareTo() throws Exception {

		final IdentifiantAffaireRF i1 = new IdentifiantAffaireRF(1, 2006, 1402, 0);
		final IdentifiantAffaireRF i2 = new IdentifiantAffaireRF(6, 2006, 1402, 0);
		final IdentifiantAffaireRF i3 = new IdentifiantAffaireRF(6, 2006, 1410, 0);
		final IdentifiantAffaireRF i4 = new IdentifiantAffaireRF(6, null, null, null);

		// numéro d'office en premier
		assertTrue(i1.compareTo(i2) < 0);
		assertTrue(i1.compareTo(i3) < 0);
		assertTrue(i1.compareTo(i4) < 0);

		// tri sur le numéro d'affaire
		assertTrue(i2.compareTo(i1) > 0);
		assertTrue(i2.compareTo(i3) < 0);
		assertTrue(i2.compareTo(i4) > 0);

		// tri sur le numéro d'affaire
		assertTrue(i3.compareTo(i1) > 0);
		assertTrue(i3.compareTo(i2) > 0);
		assertTrue(i3.compareTo(i4) > 0);

		// numéro d'affaire nuls en premier
		assertTrue(i4.compareTo(i1) > 0);
		assertTrue(i4.compareTo(i2) < 0);
		assertTrue(i4.compareTo(i3) < 0);
	}
}