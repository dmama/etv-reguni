package ch.vd.unireg.validation.bouclement;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class BouclementValidatorTest extends AbstractValidatorTest<Bouclement> {

	@Override
	protected String getValidatorBeanName() {
		return "bouclementValidator";
	}

	@Test
	public void testDateDebut() {
		final Bouclement b = new Bouclement();
		b.setDateDebut(null);
		b.setAncrage(DayMonth.get(12, 31));
		b.setPeriodeMois(2);

		final ValidationResults vr = validate(b);
		Assert.assertNotNull(vr);
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals("La date de début est obligatoire sur une donnée de bouclement.", vr.getErrors().get(0));

		// mais si on l'annule, tout va bien
		b.setAnnule(true);
		final ValidationResults vrAnnule = validate(b);
		Assert.assertNotNull(vrAnnule);
		Assert.assertEquals(0, vrAnnule.errorsCount());
		Assert.assertEquals(0, vrAnnule.warningsCount());

		// et si on met une date de début, tout va bien aussi
		b.setAnnule(false);
		b.setDateDebut(date(2000, 1, 1));
		final ValidationResults vrAvecInfo = validate(b);
		Assert.assertNotNull(vrAvecInfo);
		Assert.assertEquals(0, vrAvecInfo.errorsCount());
		Assert.assertEquals(0, vrAvecInfo.warningsCount());
	}

	@Test
	public void testAncrage() {
		final Bouclement b = new Bouclement();
		b.setDateDebut(date(2000, 5, 14));
		b.setAncrage(null);
		b.setPeriodeMois(2);

		final ValidationResults vr = validate(b);
		Assert.assertNotNull(vr);
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals("L'ancrage est obligatoire sur une donnée de bouclement.", vr.getErrors().get(0));

		// mais si on l'annule, tout va bien
		b.setAnnule(true);
		final ValidationResults vrAnnule = validate(b);
		Assert.assertNotNull(vrAnnule);
		Assert.assertEquals(0, vrAnnule.errorsCount());
		Assert.assertEquals(0, vrAnnule.warningsCount());


		// et si on met une valeur, tout va bien aussi
		b.setAnnule(false);
		b.setAncrage(DayMonth.get(12, 31));
		final ValidationResults vrAvecInfo = validate(b);
		Assert.assertNotNull(vrAvecInfo);
		Assert.assertEquals(0, vrAvecInfo.errorsCount());
		Assert.assertEquals(0, vrAvecInfo.warningsCount());
	}

	@Test
	public void testPeriodeMois() {
		final Bouclement b = new Bouclement();
		b.setDateDebut(date(2000, 5, 14));
		b.setAncrage(DayMonth.get(6, 30));

		for (int i = -10; i < 120 ; ++ i) {
			b.setAnnule(false);
			b.setPeriodeMois(i);

			final ValidationResults vr = validate(b);
			Assert.assertNotNull(vr);
			if (i < 1 || i > 99) {
				Assert.assertEquals("Periode " + i, 1, vr.errorsCount());
				Assert.assertEquals("Periode " + i, 0, vr.warningsCount());
				Assert.assertEquals("La période (en mois) doit être comprise entre 1 et 99 (" + i + ").", vr.getErrors().get(0));
			}
			else {
				Assert.assertEquals("Periode " + i, 0, vr.errorsCount());
				Assert.assertEquals("Periode " + i, 0, vr.warningsCount());
			}

			// et si on l'annule, tout va toujours bien
			b.setAnnule(true);
			final ValidationResults vrAnnule = validate(b);
			Assert.assertNotNull(vrAnnule);
			Assert.assertEquals("Periode " + i, 0, vrAnnule.errorsCount());
			Assert.assertEquals("Periode " + i, 0, vrAnnule.warningsCount());
		}
	}
}
