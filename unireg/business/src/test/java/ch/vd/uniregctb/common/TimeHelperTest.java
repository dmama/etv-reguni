package ch.vd.uniregctb.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TimeHelperTest extends WithoutSpringTest {

	@Test
	public void testPluralize() {
		assertEquals("heure", TimeHelper.pluralize(0, "heure"));
		assertEquals("heure", TimeHelper.pluralize(1, "heure"));
		assertEquals("heures", TimeHelper.pluralize(2, "heure"));
	}

	@Test
	public void testArrondi() {
		assertEquals(0L, TimeHelper.arrondi(0L, 100L));
		assertEquals(0L, TimeHelper.arrondi(10L, 100L));
		assertEquals(0L, TimeHelper.arrondi(49L, 100L));
		assertEquals(100L, TimeHelper.arrondi(50L, 100L));
		assertEquals(100L, TimeHelper.arrondi(100L, 100L));
		assertEquals(100L, TimeHelper.arrondi(149L, 100L));
		assertEquals(200L, TimeHelper.arrondi(150L, 100L));

		assertEquals(-100L, TimeHelper.arrondi(-100L, 100L));
		assertEquals(-100L, TimeHelper.arrondi(-90L, 100L));
		assertEquals(-100L, TimeHelper.arrondi(-51L, 100L));
		assertEquals(0L, TimeHelper.arrondi(-50L, 100L));
		assertEquals(0L, TimeHelper.arrondi(-10L, 100L));
	}

	@Test
	public void testFormatDuree() {
		assertEquals("0 seconde", TimeHelper.formatDuree(0, 0, 0, 0));
		assertEquals("1 seconde", TimeHelper.formatDuree(0, 0, 0, 1));
		assertEquals("2 secondes", TimeHelper.formatDuree(0, 0, 0, 2));

		assertEquals("1 minute", TimeHelper.formatDuree(0, 0, 1, 0));
		assertEquals("2 minutes", TimeHelper.formatDuree(0, 0, 2, 0));

		assertEquals("1 heure et 2 minutes", TimeHelper.formatDuree(0, 1, 2, 0));
		assertEquals("2 heures et 10 secondes", TimeHelper.formatDuree(0, 2, 0, 10));

		assertEquals("1 jour, 23 minutes et 1 seconde", TimeHelper.formatDuree(1, 0, 23, 1));
		assertEquals("2 jours, 10 heures, 1 minute et 45 secondes", TimeHelper.formatDuree(2, 10, 1, 45));
	}

	@Test
	public void testFormatDureeShort() {
		assertEquals("0s", TimeHelper.formatDureeShort(0, 0, 0, 0));
		assertEquals("1s", TimeHelper.formatDureeShort(0, 0, 0, 1));
		assertEquals("2s", TimeHelper.formatDureeShort(0, 0, 0, 2));

		assertEquals("1m 00s", TimeHelper.formatDureeShort(0, 0, 1, 0));
		assertEquals("2m 00s", TimeHelper.formatDureeShort(0, 0, 2, 0));

		assertEquals("1h 02m 00s", TimeHelper.formatDureeShort(0, 1, 2, 0));
		assertEquals("2h 00m 10s", TimeHelper.formatDureeShort(0, 2, 0, 10));

		assertEquals("1j 00h 23m 01s", TimeHelper.formatDureeShort(1, 0, 23, 1));
		assertEquals("2j 10h 01m 45s", TimeHelper.formatDureeShort(2, 10, 1, 45));
	}

	@Test
	public void testFormatDureeArrondie() {
		assertEquals("0 seconde", TimeHelper.formatDureeArrondie(0L));
		assertEquals("1 seconde", TimeHelper.formatDureeArrondie(1000L));
		assertEquals("2 secondes", TimeHelper.formatDureeArrondie(2000L));

		assertEquals("1 minute", TimeHelper.formatDureeArrondie(60000L));
		assertEquals("2 minutes", TimeHelper.formatDureeArrondie(120000L));
		assertEquals("2 minutes et 20 secondes", TimeHelper.formatDureeArrondie(140000L));
		assertEquals("2 minutes et 40 secondes", TimeHelper.formatDureeArrondie(160000L));
		assertEquals("5 minutes", TimeHelper.formatDureeArrondie(5 * 60000L + 20000L));            // 5 minutes et 20 secondes
		assertEquals("6 minutes", TimeHelper.formatDureeArrondie(5 * 60000L + 40000L));            // 5 minutes et 40 secondes

		assertEquals("1 heure et 2 minutes", TimeHelper.formatDureeArrondie(3600000L + 120000L));
		assertEquals("2 heures", TimeHelper.formatDureeArrondie(7200000L + 10000L));                           // 2 heures et 10 secondes
		assertEquals("4 heures et 50 minutes", TimeHelper.formatDureeArrondie(4 * 3600000L + 50 * 60000L));    // 4 heures et 50 secondes
		assertEquals("6 heures", TimeHelper.formatDureeArrondie(5 * 3600000L + 50 * 60000L));                  // 5 heures et 50 minutes
		assertEquals("23 heures", TimeHelper.formatDureeArrondie(23 * 3600000L + 29 * 60000L));                // 23 heures et 29 minutes

		assertEquals("1 jour", TimeHelper.formatDureeArrondie(23 * 3600000L + 30 * 60000L));                                   // 23 heures et 30 minutes
		assertEquals("1 jour", TimeHelper.formatDureeArrondie(86400000 + 23 * 60000L + 1000L));                                // 1 jour, 23 minutes et 1 seconde
		assertEquals("2 jours et 10 heures", TimeHelper.formatDureeArrondie(2 * 86400000L + 10 * 3600000L + 60000L + 45000L)); // 2 jours, 10 heures, 1 minute et 45 secondes
		assertEquals("5 jours", TimeHelper.formatDureeArrondie(5 * 86400000L + 10 * 3600000L + 60000L + 45000L));              // 5 jours, 10 heures, 1 minute et 45 secondes
	}
}
