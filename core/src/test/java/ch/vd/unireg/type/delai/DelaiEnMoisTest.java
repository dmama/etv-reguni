package ch.vd.unireg.type.delai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DelaiEnMoisTest {

	@Test
	public void testToString() {
		assertEquals("0M", new DelaiEnMois(0, false).toString());
		assertEquals("6M", new DelaiEnMois(6, false).toString());
		assertEquals("-6M", new DelaiEnMois(-6, false).toString());
		assertEquals("6M~", new DelaiEnMois(6, true).toString());
		assertEquals("14M~", new DelaiEnMois(14, true).toString());
	}

	@Test
	public void testFromString() {
		assertEquals(new DelaiEnMois(0, false), DelaiEnMois.fromString("0M"));
		assertEquals(new DelaiEnMois(6, false), DelaiEnMois.fromString("6M"));
		assertEquals(new DelaiEnMois(-6, false), DelaiEnMois.fromString("-6M"));
		assertEquals(new DelaiEnMois(14, false), DelaiEnMois.fromString("14M"));
		assertEquals(new DelaiEnMois(6, true), DelaiEnMois.fromString("6M~"));
		assertEquals(new DelaiEnMois(14, true), DelaiEnMois.fromString("14M~"));
	}
}