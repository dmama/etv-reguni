package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertFalse;

public class ForFiscalAutreImpotTest extends WithoutSpringTest {

	@Test
	public void testValidateForAnnule() {

		final ForFiscalAutreImpot forFiscal = new ForFiscalAutreImpot();

		// For invalide (genre impôt incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}
	}

}
