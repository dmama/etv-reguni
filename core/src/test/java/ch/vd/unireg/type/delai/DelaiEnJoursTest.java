package ch.vd.unireg.type.delai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DelaiEnJoursTest {

	@Test
	public void testToString() {
		assertEquals("0D", new DelaiEnJours(0, false).toString());
		assertEquals("6D", new DelaiEnJours(6, false).toString());
		assertEquals("-6D", new DelaiEnJours(-6, false).toString());
		assertEquals("255D", new DelaiEnJours(255, false).toString());
		assertEquals("6D~", new DelaiEnJours(6, true).toString());
		assertEquals("14D~", new DelaiEnJours(14, true).toString());
		assertEquals("255D~", new DelaiEnJours(255, true).toString());
	}

	@Test
	public void testFromString() {
		assertEquals(new DelaiEnJours(0, false), DelaiEnJours.fromString("0D"));
		assertEquals(new DelaiEnJours(6, false), DelaiEnJours.fromString("6D"));
		assertEquals(new DelaiEnJours(-6, false), DelaiEnJours.fromString("-6D"));
		assertEquals(new DelaiEnJours(14, false), DelaiEnJours.fromString("14D"));
		assertEquals(new DelaiEnJours(255, false), DelaiEnJours.fromString("255D"));
		assertEquals(new DelaiEnJours(6, true), DelaiEnJours.fromString("6D~"));
		assertEquals(new DelaiEnJours(14, true), DelaiEnJours.fromString("14D~"));
		assertEquals(new DelaiEnJours(255, true), DelaiEnJours.fromString("255D~"));
	}
}