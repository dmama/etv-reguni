package ch.vd.unireg.etiquette;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;

public class DecalageAvecCorrectionTest extends WithoutSpringTest {

	@Test
	public void testDecalageSansCorrection() throws Exception {
		final DecalageAvecCorrection decalage = new DecalageAvecCorrection(5, UniteDecalageDate.JOUR, CorrectionSurDate.SANS_CORRECTION);
		final RegDate dateDepart = date(2000, 1, 1);
		Assert.assertNull(decalage.apply(null));
		for (int index = 0 ; index < 1000 ; ++ index) {
			final RegDate dateReference = dateDepart.addDays(index);
			final RegDate expected = dateReference.addDays(5);
			Assert.assertEquals(expected, decalage.apply(dateReference));
		}
	}

	@Test
	public void testDecalageAvecCorrection() throws Exception {
		final DecalageAvecCorrection decalage = new DecalageAvecCorrection(3, UniteDecalageDate.SEMAINE, CorrectionSurDate.FIN_MOIS);
		final RegDate dateDepart = date(2000, 1, 1);
		Assert.assertNull(decalage.apply(null));
		for (int index = 0 ; index < 1000 ; ++ index) {
			final RegDate dateReference = dateDepart.addDays(index);
			final RegDate expected = dateReference.addDays(21).getLastDayOfTheMonth();
			Assert.assertEquals(expected, decalage.apply(dateReference));
		}
	}
}
