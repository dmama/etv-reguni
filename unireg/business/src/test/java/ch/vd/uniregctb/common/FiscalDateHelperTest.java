package ch.vd.uniregctb.common;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;

public class FiscalDateHelperTest extends WithoutSpringTest {

	protected static final Logger LOGGER = Logger.getLogger(FiscalDateHelperTest.class);

	@Test
	public void testGetAnneeCourante() {
		// Standard cases
		GregorianCalendar cal = new GregorianCalendar(1990, 4, 4);
		assertEquals(1990, FiscalDateHelper.getAnneeCourante(cal));
		cal = new GregorianCalendar(1995, 10, 11);
		assertEquals(1995, FiscalDateHelper.getAnneeCourante(cal));

		// Border case
		cal = new GregorianCalendar(1998, 11, 20);
		assertEquals(1998, FiscalDateHelper.getAnneeCourante(cal));

		// After end of fiscal year
		cal = new GregorianCalendar(1998, 11, 21);
		assertEquals(1999, FiscalDateHelper.getAnneeCourante(cal));
		cal = new GregorianCalendar(1998, 11, 28);
		assertEquals(1999, FiscalDateHelper.getAnneeCourante(cal));
		cal = new GregorianCalendar(1998, 11, 31);
		assertEquals(1999, FiscalDateHelper.getAnneeCourante(cal));

		// Back to standard case
		cal = new GregorianCalendar(1999, 1, 1);
		assertEquals(1999, FiscalDateHelper.getAnneeCourante(cal));
	}

	@Test
	public void testGetDateFiscale() {
		// Standard cases
		assertEquals(DateHelper.getDate(1991, 2, 4), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1991, 2, 4)));
		assertEquals(DateHelper.getDate(1994, 5, 23), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1994, 5, 23)));
		assertEquals(DateHelper.getDate(2008, 2, 18), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(2008, 2, 18)));

		// Border cases
		assertEquals(DateHelper.getDate(1990, 12, 19), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1990, 12, 19)));
		assertEquals(DateHelper.getDate(1990, 12, 20), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1990, 12, 20)));
		assertEquals(DateHelper.getDate(1991, 1, 1), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1990, 12, 21)));
		assertEquals(DateHelper.getDate(1991, 1, 1), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1990, 12, 25)));
		assertEquals(DateHelper.getDate(1991, 1, 1), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1990, 12, 31)));
		assertEquals(DateHelper.getDate(1991, 1, 1), FiscalDateHelper.getDateEvenementFiscal(DateHelper.getDate(1991, 1, 1)));
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

	private static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}

	private static RegDate date(int year, int month) {
		return RegDate.get(year, month);
	}

	private static RegDate date(int year) {
		return RegDate.get(year);
	}
}
