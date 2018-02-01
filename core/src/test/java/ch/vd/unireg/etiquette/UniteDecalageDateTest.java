package ch.vd.unireg.etiquette;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;

public class UniteDecalageDateTest extends WithoutSpringTest {

	@Test
	public void testAnnee() throws Exception {
		final UniteDecalageDate udd = UniteDecalageDate.ANNEE;
		final RegDate dateDepart = date(2000, 1, 1);
		for (int days = 0 ; days < 1000 ; ++ days) {
			for (int i = -50 ; i < 50 ; ++ i) {
				final RegDate date = dateDepart.addDays(days);
				final RegDate expected = date.addYears(i);
				Assert.assertEquals(String.format("%d/%d", days, i), expected, udd.apply(date, i));
			}
		}
	}

	@Test
	public void testMois() throws Exception {
		final UniteDecalageDate udd = UniteDecalageDate.MOIS;
		final RegDate dateDepart = date(2000, 1, 1);
		for (int days = 0 ; days < 1000 ; ++ days) {
			for (int i = -50 ; i < 50 ; ++ i) {
				final RegDate date = dateDepart.addDays(days);
				final RegDate expected = date.addMonths(i);
				Assert.assertEquals(String.format("%d/%d", days, i), expected, udd.apply(date, i));
			}
		}
	}

	@Test
	public void testSemaine() throws Exception {
		final UniteDecalageDate udd = UniteDecalageDate.SEMAINE;
		final RegDate dateDepart = date(2000, 1, 1);
		for (int days = 0 ; days < 1000 ; ++ days) {
			for (int i = -50 ; i < 50 ; ++ i) {
				final RegDate date = dateDepart.addDays(days);
				final RegDate expected = date.addDays(7 * i);
				Assert.assertEquals(String.format("%d/%d", days, i), expected, udd.apply(date, i));
			}
		}
	}

	@Test
	public void testJour() throws Exception {
		final UniteDecalageDate udd = UniteDecalageDate.JOUR;
		final RegDate dateDepart = date(2000, 1, 1);
		for (int days = 0 ; days < 1000 ; ++ days) {
			for (int i = -50 ; i < 50 ; ++ i) {
				final RegDate date = dateDepart.addDays(days);
				final RegDate expected = date.addDays(i);
				Assert.assertEquals(String.format("%d/%d", days, i), expected, udd.apply(date, i));
			}
		}
	}
}
