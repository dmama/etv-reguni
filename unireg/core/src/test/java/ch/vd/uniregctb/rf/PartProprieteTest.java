package ch.vd.uniregctb.rf;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PartProprieteTest {

	@Test
	public void testParseOK() throws Exception {
		assertNull(PartPropriete.parse(" "));
		assertEquals(new PartPropriete(1, 1), PartPropriete.parse("1"));
		assertEquals(new PartPropriete(1, 2), PartPropriete.parse("1/2"));
		assertEquals(new PartPropriete(2, 3), PartPropriete.parse("2 /3 "));
	}

	@Test
	public void testParseException() throws Exception {
		try {
			PartPropriete.parse("3");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [3] ne représente par une part de propriété valide", e.getMessage());
		}
		try {
			PartPropriete.parse("1/");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [1/] ne représente par une part de propriété valide", e.getMessage());
		}
		try {
			PartPropriete.parse("4/5/7");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La string [4/5/7] ne représente par une part de propriété valide", e.getMessage());
		}
	}
}
