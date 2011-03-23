package ch.vd.uniregctb.validation.fors;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class ForFiscalAutreImpotValidatorTest extends AbstractValidatorTest<ForFiscalAutreImpot> {

	@Test
	public void testValidateForAnnule() {

		final ForFiscalAutreImpot forFiscal = new ForFiscalAutreImpot();

		// For invalide (genre impôt incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}
	}

	@Override
	protected String getValidatorBeanName() {
		return "forFiscalAutreImpotValidator";
	}
}
