package ch.vd.uniregctb.validation.fors;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class ForFiscalAutreElementImposableValidatorTest extends AbstractValidatorTest<ForFiscalAutreElementImposable> {

	@Test
	public void testValidateForAnnule() {

		final ForFiscalAutreElementImposable forFiscal = new ForFiscalAutreElementImposable();

		// For invalide (type d'autorité fiscale incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}
	}

	@Override
	protected String getValidatorBeanName() {
		return "forFiscalAutreElementImposableValidator";
	}
}