package ch.vd.unireg.type.delai;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DelaiCompositeTest {

	@Test
	public void testToString() {
		assertEquals("6M + 75D", new DelaiComposite(Arrays.asList(new DelaiEnMois(6, false), new DelaiEnJours(75, false))).toString());
		assertEquals("6M~ + 75D~", new DelaiComposite(Arrays.asList(new DelaiEnMois(6, true), new DelaiEnJours(75, true))).toString());
		assertEquals("6M~ + 3M + 75D", new DelaiComposite(Arrays.asList(new DelaiEnMois(6, true), new DelaiEnMois(3, false), new DelaiEnJours(75, false))).toString());
	}

	@Test
	public void testFromString() {
		assertEquals(new DelaiComposite(Arrays.asList(new DelaiEnMois(6, false), new DelaiEnJours(75, false))), DelaiComposite.fromString("6M + 75D"));
		assertEquals(new DelaiComposite(Arrays.asList(new DelaiEnMois(6, true), new DelaiEnJours(75, true))), DelaiComposite.fromString("6M~ + 75D~"));
		assertEquals(new DelaiComposite(Arrays.asList(new DelaiEnMois(6, true), new DelaiEnMois(3, false), new DelaiEnJours(75, false))), DelaiComposite.fromString("6M~ + 3M + 75D"));
	}
}