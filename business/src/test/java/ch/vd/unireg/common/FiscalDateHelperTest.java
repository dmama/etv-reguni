package ch.vd.unireg.common;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeHelper;
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

	@Test
	public void testGetLongueurEnJours() {

		// toujours 1 jour pour un intervalle d'un jour
		for (RegDate date = date(2000, 1, 1); date.compareTo(date(2005, 12, 31)) <= 0 ; date = date.getOneDayAfter()) {
			final DateRangeHelper.Range range = new DateRangeHelper.Range(date, date);
			assertEquals(range.toString(), 1, FiscalDateHelper.getLongueurEnJours(range));
		}

		// toujours 30 jours pour un intervalle d'un mois
		for (RegDate date = date(2000, 1, 1); date.compareTo(date(2005, 12, 31)) <= 0 ; date = date.addMonths(1)) {
			final DateRangeHelper.Range range = new DateRangeHelper.Range(date, date.getLastDayOfTheMonth());
			assertEquals(range.toString(), 30, FiscalDateHelper.getLongueurEnJours(range));
		}

		// toujours 60 jours pour un intervalle de deux mois
		for (RegDate date = date(2000, 1, 1); date.compareTo(date(2005, 12, 31)) <= 0 ; date = date.addMonths(1)) {
			final DateRangeHelper.Range range = new DateRangeHelper.Range(date, date.addMonths(1).getLastDayOfTheMonth());
			assertEquals(range.toString(), 60, FiscalDateHelper.getLongueurEnJours(range));
		}

		// toujours 90 jours pour un intervalle de trois mois
		for (RegDate date = date(2000, 1, 1); date.compareTo(date(2005, 12, 31)) <= 0 ; date = date.addMonths(1)) {
			final DateRangeHelper.Range range = new DateRangeHelper.Range(date, date.addMonths(2).getLastDayOfTheMonth());
			assertEquals(range.toString(), 90, FiscalDateHelper.getLongueurEnJours(range));
		}

		// toujours 180 jours pour un intervalle de six mois
		for (RegDate date = date(2000, 1, 1); date.compareTo(date(2005, 12, 31)) <= 0 ; date = date.addMonths(1)) {
			final DateRangeHelper.Range range = new DateRangeHelper.Range(date, date.addMonths(5).getLastDayOfTheMonth());
			assertEquals(range.toString(), 180, FiscalDateHelper.getLongueurEnJours(range));
		}

		// quelques cas à la con
		assertEquals("1.1 -> 30.1", 30, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 1, 30))));
		assertEquals("1.1 -> 31.1", 30, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 1, 31))));
		assertEquals("1.1 -> 1.2", 31, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 2, 1))));
		assertEquals("1.1 -> 2.2", 32, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 2, 2))));
		assertEquals("1.1 -> 28.2", 60, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 2, 28))));
		assertEquals("1.1 -> 1.3", 61, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 3, 1))));
		assertEquals("1.1 -> 2.3", 62, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 1, 1), date(2001, 3, 2))));

		// année non-bissextile
		assertEquals("1.2 -> 28.2", 30, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 2, 1), date(2001, 2, 28))));
		assertEquals("1.2 -> 27.2", 27, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 2, 1), date(2001, 2, 27))));
		assertEquals("1.2 -> 1.3", 31, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 2, 1), date(2001, 3, 1))));
		assertEquals("1.2 -> 2.3", 32, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2001, 2, 1), date(2001, 3, 2))));

		// année bissextile
		assertEquals("1.2 -> 29.2", 30, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2000, 2, 1), date(2000, 2, 29))));
		assertEquals("1.2 -> 28.2", 28, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2000, 2, 1), date(2000, 2, 28))));
		assertEquals("1.2 -> 27.2", 27, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2000, 2, 1), date(2000, 2, 27))));
		assertEquals("1.2 -> 1.3", 31, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2000, 2, 1), date(2000, 3, 1))));
		assertEquals("1.2 -> 2.3", 32, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2000, 2, 1), date(2000, 3, 2))));

		// cas fournis par [SIFISC-18066]
		assertEquals("18.04 -> 31.12", 253, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2015, 4, 18), date(2015, 12, 31))));
		assertEquals("13.10 -> 31.12", 78, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2015, 10, 13), date(2015, 12, 31))));
		assertEquals("15.05 -> 31.12", 226, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2015, 5, 15), date(2015, 12, 31))));
		assertEquals("18.02 -> 31.12", 313, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2015, 2, 18), date(2015, 12, 31))));
		assertEquals("18.02 (bissextile) -> 31.12", 313, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2016, 2, 18), date(2016, 12, 31))));
		assertEquals("28.02 -> 31.12", 301, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2015, 2, 28), date(2015, 12, 31))));
		assertEquals("28.02 (bissextile) -> 31.12", 303, FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2016, 2, 28), date(2016, 12, 31))));

		// cas pris au hasard
		assertEquals("5.12.2000 -> 18.08.2003",
		             (30 - 5 + 1) + 2 * 360 + 7 * 30 + 18,
		             FiscalDateHelper.getLongueurEnJours(new DateRangeHelper.Range(date(2000, 12, 5), date(2003, 8, 18))));
	}
}
