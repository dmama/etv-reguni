package ch.vd.unireg.etiquette;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;

public class CorrectionSurDateTest extends WithoutSpringTest {

	@Test
	public void testFinAnneePrecedente() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.FIN_ANNEE_PRECEDENTE;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date(date.year() - 1, 12, 31);
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testDebutAnnee() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.DEBUT_ANNEE;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date(date.year(), 1, 1);
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testFinMoisPrecedent() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.FIN_MOIS_PRECEDENT;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date(date.year(), date.month(), 1).getOneDayBefore();
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testDebutMois() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.DEBUT_MOIS;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date(date.year(), date.month(), 1);
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testSansCorrection() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.SANS_CORRECTION;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			Assert.assertEquals(date, csd.apply(date));
		}
	}

	@Test
	public void testFinMois() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.FIN_MOIS;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date.getLastDayOfTheMonth();
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testDebutMoisSuivant() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.DEBUT_MOIS_SUIVANT;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date.getLastDayOfTheMonth().getOneDayAfter();
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testFinAnnee() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.FIN_ANNEE;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date(date.year(), 12, 31);
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}

	@Test
	public void testDebutAnneeSuivante() throws Exception {
		final CorrectionSurDate csd = CorrectionSurDate.DEBUT_ANNEE_SUIVANTE;
		final RegDate dateDepart = date(2000, 1, 1);
		final int nbJoursMax = 1000;
		Assert.assertNull(csd.apply(null));
		for (int index = 0 ; index < nbJoursMax ; ++ index) {
			final RegDate date = dateDepart.addDays(index);
			final RegDate transformee =  date(date.year() + 1, 1, 1);
			Assert.assertEquals(transformee, csd.apply(date));
		}
	}
}
