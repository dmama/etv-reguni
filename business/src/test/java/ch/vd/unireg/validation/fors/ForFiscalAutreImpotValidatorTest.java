package ch.vd.unireg.validation.fors;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.ForFiscalAutreImpot;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class ForFiscalAutreImpotValidatorTest extends AbstractValidatorTest<ForFiscalAutreImpot> {

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
