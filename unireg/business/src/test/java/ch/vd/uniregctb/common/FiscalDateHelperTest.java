package ch.vd.uniregctb.common;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FiscalDateHelperTest extends WithoutSpringTest {

	protected static final Logger LOGGER = LoggerFactory.getLogger(FiscalDateHelperTest.class);

	@Test
	public void testGetAnneeCourante() {
		// Standard cases
		assertEquals(1990, FiscalDateHelper.getAnneeFiscale(date(1990, 4, 4)));
		assertEquals(1995, FiscalDateHelper.getAnneeFiscale(date(1995, 10, 11)));

		// Border case
		assertEquals(1998, FiscalDateHelper.getAnneeFiscale(date(1998, 12, 20)));

		// After end of fiscal year
		assertEquals(1999, FiscalDateHelper.getAnneeFiscale(date(1998, 12, 21)));
		assertEquals(1999, FiscalDateHelper.getAnneeFiscale(date(1998, 12, 28)));
		assertEquals(1999, FiscalDateHelper.getAnneeFiscale(date(1998, 12, 31)));

		// Back to standard case
		assertEquals(1999, FiscalDateHelper.getAnneeFiscale(date(1999, 1, 1)));
	}

	@Test
	public void testIsMajeur() {

		final RegDate reference = date(2000, 1, 1);

		// Dates complètes
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1902, 1, 1))); // 98 ans
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1980, 1, 1))); // 20 ans
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1982, 1, 1))); // juste 18 ans

		assertFalse(FiscalDateHelper.isMajeur(reference, date(1982, 1, 2))); // 18 ans - 1 jour
		assertFalse(FiscalDateHelper.isMajeur(reference, date(1999, 12, 31))); // 1 jour
		assertFalse(FiscalDateHelper.isMajeur(reference, date(2020, 1, 1))); // -20 ans

		// Dates partielles (year/month)
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1980, 1))); // 20 ans
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1982, 1))); // 18 ans

		assertFalse(FiscalDateHelper.isMajeur(reference, date(1982, 2))); // 18 ans - 1 mois
		assertFalse(FiscalDateHelper.isMajeur(reference, date(1999, 12))); // 1 mois
		assertFalse(FiscalDateHelper.isMajeur(reference, date(2020, 1))); // -20 ans

		// Dates partielles (year)
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1980))); // 20 ans
		assertTrue(FiscalDateHelper.isMajeur(reference, date(1982))); // 18 ans

		assertFalse(FiscalDateHelper.isMajeur(reference, date(1983))); // 17 ans
		assertFalse(FiscalDateHelper.isMajeur(reference, date(1999))); // 1 an
		assertFalse(FiscalDateHelper.isMajeur(reference, date(2020))); // -20 ans
	}

	@Test
	public void testGetDateMajorite() {
		assertEquals(date(2018, 4, 3), FiscalDateHelper.getDateMajorite(date(2000, 4, 3)));
		assertEquals(date(1992, 2, 23), FiscalDateHelper.getDateMajorite(date(1974, 2, 23)));
		assertEquals(date(2018, 2, 28), FiscalDateHelper.getDateMajorite(date(2000, 2, 29))); // année bissextile

		assertEquals(date(2018, 4, 1), FiscalDateHelper.getDateMajorite(date(2000, 4))); // date partielle
		assertEquals(date(2018, 1, 1), FiscalDateHelper.getDateMajorite(date(2000))); // date partielle
	}

	@Test
	public void testGetDateComplete() {
		assertEquals(date(2000, 5, 2), FiscalDateHelper.getDateComplete(date(2000, 5, 2)));
		assertEquals(date(2000, 5, 1), FiscalDateHelper.getDateComplete(date(2000, 5)));
		assertEquals(date(2000, 1, 1), FiscalDateHelper.getDateComplete(date(2000)));
	}
}
