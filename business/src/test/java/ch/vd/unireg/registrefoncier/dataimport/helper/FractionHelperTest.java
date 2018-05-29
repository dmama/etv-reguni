package ch.vd.unireg.registrefoncier.dataimport.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.unireg.registrefoncier.Fraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FractionHelperTest {

	/**
	 * Ce test vérifie que deux fractions identiques sont bien considérées comme égales.
	 */
	@Test
	public void testDataEquals() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(2);
		fraction.setDenominateur(5);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertTrue(FractionHelper.dataEquals(fraction, quote));
	}

	/**
	 * Ce test vérifie les différents cas de nullité possibles.
	 */
	@Test
	public void testDataEqualsNulles() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(2);
		fraction.setDenominateur(5);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertFalse(FractionHelper.dataEquals(null, quote));
		assertFalse(FractionHelper.dataEquals(fraction, null));
		assertTrue(FractionHelper.dataEquals(null, null));
	}

	/**
	 * Ce test vérifie que deux fractions avec des numérateurs différents sont bien considérées comme inégales.
	 */
	@Test
	public void testDataEqualsNumerateursDifferents() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(5);
		fraction.setDenominateur(5);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertFalse(FractionHelper.dataEquals(fraction, quote));
	}

	/**
	 * Ce test vérifie que deux fractions avec des numérateurs différents sont bien considérées comme inégales.
	 */
	@Test
	public void testDataEqualsDenominateursDifferents() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(2);
		fraction.setDenominateur(7);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertFalse(FractionHelper.dataEquals(fraction, quote));
	}

	@Test
	public void testGet() throws Exception {
		assertNull(FractionHelper.get(null));
		assertEquals(new Fraction(2, 5), FractionHelper.get(new Quote(2L, 5L, null, null)));
		assertEquals(new Fraction(15, 100), FractionHelper.get(new Quote(null, null, "15%", null)));
		assertNull(FractionHelper.get(new Quote(null, null, "deux pourcents", null)));
	}
}