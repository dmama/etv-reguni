package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.uniregctb.registrefoncier.Fraction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FractionHelperTest {

	/**
	 * Ce test vérifie que deux fractions identiques sont bien considérées comme égales.
	 */
	@Test
	public void testFractionEquals() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(2);
		fraction.setDenominateur(5);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertTrue(FractionHelper.fractionEquals(fraction, quote));
	}

	/**
	 * Ce test vérifie les différents cas de nullité possibles.
	 */
	@Test
	public void testFractionEqualsNulles() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(2);
		fraction.setDenominateur(5);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertFalse(FractionHelper.fractionEquals(null, quote));
		assertFalse(FractionHelper.fractionEquals(fraction, null));
		assertTrue(FractionHelper.fractionEquals(null, null));
	}

	/**
	 * Ce test vérifie que deux fractions avec des numérateurs différents sont bien considérées comme inégales.
	 */
	@Test
	public void testFractionEqualsNumerateursDifferents() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(5);
		fraction.setDenominateur(5);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertFalse(FractionHelper.fractionEquals(fraction, quote));
	}

	/**
	 * Ce test vérifie que deux fractions avec des numérateurs différents sont bien considérées comme inégales.
	 */
	@Test
	public void testFractionEqualsDenominateursDifferents() throws Exception {

		final Fraction fraction = new Fraction();
		fraction.setNumerateur(2);
		fraction.setDenominateur(7);

		final Quote quote = new Quote();
		quote.setAnteilZaehler(2L);
		quote.setAnteilNenner(5L);

		assertFalse(FractionHelper.fractionEquals(fraction, quote));
	}
}