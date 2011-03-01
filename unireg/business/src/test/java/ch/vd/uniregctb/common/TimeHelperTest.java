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
	public void testFormatDureeExecution() {
		assertEquals("0 seconde", TimeHelper.formatDuree(0, 0, 0, 0));
		assertEquals("1 seconde", TimeHelper.formatDuree(0, 0, 0, 1));
		assertEquals("2 secondes", TimeHelper.formatDuree(0, 0, 0, 2));

		assertEquals("1 minute et 0 seconde", TimeHelper.formatDuree(0, 0, 1, 0));
		assertEquals("2 minutes et 0 seconde", TimeHelper.formatDuree(0, 0, 2, 0));

		assertEquals("1 heure, 2 minutes et 0 seconde", TimeHelper.formatDuree(0, 1, 2, 0));
		assertEquals("2 heures, 0 minute et 10 secondes", TimeHelper.formatDuree(0, 2, 0, 10));

		assertEquals("1 jour, 0 heure, 23 minutes et 1 seconde", TimeHelper.formatDuree(1, 0, 23, 1));
		assertEquals("2 jours, 10 heures, 1 minute et 45 secondes", TimeHelper.formatDuree(2, 10, 1, 45));
	}
}
