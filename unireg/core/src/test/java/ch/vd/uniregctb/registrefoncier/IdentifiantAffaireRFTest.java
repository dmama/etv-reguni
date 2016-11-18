package ch.vd.uniregctb.registrefoncier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IdentifiantAffaireRFTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("006-2006/1402/0", new IdentifiantAffaireRF(6, 2006, 1402, 0).toString());
		assertEquals("006", new IdentifiantAffaireRF(6, null, null, null).toString());
	}

	@Test
	public void testParse() throws Exception {

		assertEquals(new IdentifiantAffaireRF(6, 2006, 1402, 0), IdentifiantAffaireRF.parse("006-2006/1402/0"));
		assertEquals(new IdentifiantAffaireRF(6, null, null, null), IdentifiantAffaireRF.parse("006"));

		try {
			IdentifiantAffaireRF.parse("6-2006/1402/0");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [6-2006/1402/0] ne représente pas un identifiant d'affaire RF valide", e.getMessage());
		}

		try {
			IdentifiantAffaireRF.parse("01-1998/3535");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [01-1998/3535] ne représente pas un identifiant d'affaire RF valide", e.getMessage());
		}
	}
}