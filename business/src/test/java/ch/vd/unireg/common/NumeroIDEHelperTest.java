package ch.vd.unireg.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NumeroIDEHelperTest extends WithoutSpringTest {

	@Test
	public void testCalculationOfControlDigit() throws Exception {
		assertEquals(6, NumeroIDEHelper.computeControlDigit("99999999"));       // Exemple du site www.bfs.admin.ch
		assertEquals(0, NumeroIDEHelper.computeControlDigit("11626765"));       // Jumbo
		assertEquals(5, NumeroIDEHelper.computeControlDigit("11631118"));       // Coop
		assertEquals(1, NumeroIDEHelper.computeControlDigit("10596039"));       // Agip
		assertEquals(2, NumeroIDEHelper.computeControlDigit("10624451"));       // Landi Gros-de-Vaud
		assertEquals(3, NumeroIDEHelper.computeControlDigit("42911124"));       // Fly
		assertEquals(0, NumeroIDEHelper.computeControlDigit("10000100"));       // ?
		assertEquals(0, NumeroIDEHelper.computeControlDigit("10000337"));       // ?
		assertEquals(0, NumeroIDEHelper.computeControlDigit("10001101"));       // ?
	}

	@Test
	public void testNormalize() throws Exception {
		assertEquals("CHE123456789", NumeroIDEHelper.normalize("CHE-123.456.789"));
		assertEquals("CHE123456789", NumeroIDEHelper.normalize("CHE-123.456 789"));
		assertEquals("CHE123456789", NumeroIDEHelper.normalize("CHE123456789"));
		assertEquals("CHE123456789", NumeroIDEHelper.normalize("cHe-123.456.789"));
		assertEquals("CHE123456789", NumeroIDEHelper.normalize("chE-123.456 789"));
		assertEquals("CHE123456789", NumeroIDEHelper.normalize("che123456789"));
		assertNull(NumeroIDEHelper.normalize(null));
		assertNull(NumeroIDEHelper.normalize(""));
		assertNull(NumeroIDEHelper.normalize("  .. -  "));
	}

	@Test
	public void testFormatNumeroIDE() throws Exception {
		assertEquals("CHE-114.002.069", NumeroIDEHelper.formater("CHE114002069"));
	}

	@Test
	public void testIsValid() throws Exception {
		assertFalse(NumeroIDEHelper.isValid("..-..-"));
		assertFalse(NumeroIDEHelper.isValid(null));

		assertFalse(NumeroIDEHelper.isValid("ZZZ-999.999.996"));
		assertFalse(NumeroIDEHelper.isValid("ZZZ999999996"));
		assertTrue(NumeroIDEHelper.isValid("ADM-999.999.996"));
		assertTrue(NumeroIDEHelper.isValid("ADM999999996"));
		assertTrue(NumeroIDEHelper.isValid("CHE-999.999.996"));
		assertTrue(NumeroIDEHelper.isValid("CHE999999996"));

		assertTrue(NumeroIDEHelper.isValid("CHE-116.267.650"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.651"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.652"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.653"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.654"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.655"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.656"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.657"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.658"));
		assertFalse(NumeroIDEHelper.isValid("CHE-116.267.659"));

		assertFalse(NumeroIDEHelper.isValid("CHE-123.456.720"));        // (11 - le modulo 11) donne 10, ce qui est considéré comme un mauvais numéro
	}
}
