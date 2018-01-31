package ch.vd.uniregctb.registrefoncier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IdentifiantDroitRFTest {

	@Test
	public void testToString() throws Exception {
		assertEquals("001-1998/003535", new IdentifiantDroitRF(1, 1998, 3535).toString());
	}

	@Test
	public void testParse() throws Exception {

		assertEquals(new IdentifiantDroitRF(1, 1998, 3535), IdentifiantDroitRF.parse("001-1998/003535"));

		try {
			IdentifiantDroitRF.parse("1-1998/003535");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [1-1998/003535] ne représente pas un identifiant de droit RF valide", e.getMessage());
		}

		try {
			IdentifiantDroitRF.parse("01-1998/3535");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [01-1998/3535] ne représente pas un identifiant de droit RF valide", e.getMessage());
		}
	}
}