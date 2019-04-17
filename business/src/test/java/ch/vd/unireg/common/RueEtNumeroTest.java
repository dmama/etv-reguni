package ch.vd.unireg.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RueEtNumeroTest {
	@Test
	public void testFormat() {
		assertEquals("", RueEtNumero.format(null, null));
		assertEquals("", RueEtNumero.format("", null));
		assertEquals("", RueEtNumero.format("", ""));
		assertEquals("", RueEtNumero.format(" ", "       "));
		assertEquals("rue", RueEtNumero.format("rue", null));
		assertEquals("rue", RueEtNumero.format("rue", ""));
		assertEquals("rue", RueEtNumero.format("rue", "   "));
		assertEquals("rue 43", RueEtNumero.format("rue", "43"));
		assertEquals("", RueEtNumero.format(null, "43"));
		assertEquals("", RueEtNumero.format("", "43"));
		assertEquals("", RueEtNumero.format("  ", "43"));
	}
}