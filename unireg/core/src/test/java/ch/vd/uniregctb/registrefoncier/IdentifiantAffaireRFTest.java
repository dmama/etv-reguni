package ch.vd.uniregctb.registrefoncier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IdentifiantAffaireRFTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("006-2006/1402/0", new IdentifiantAffaireRF(6, 2006, 1402, 0).toString());
	}

	@Test
	public void testParse() throws Exception {

		assertEquals(new IdentifiantAffaireRF(6, 2006, 1402, 0), IdentifiantAffaireRF.parse("006-2006/1402/0"));

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