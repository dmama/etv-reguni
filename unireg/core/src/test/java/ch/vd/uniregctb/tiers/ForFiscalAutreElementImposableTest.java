package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertFalse;

public class ForFiscalAutreElementImposableTest extends WithoutSpringTest {

	@Test
	public void testValidateForAnnule() {

		final ForFiscalAutreElementImposable forFiscal = new ForFiscalAutreElementImposable();

		// For invalide (type d'autorité fiscale incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}
	}

}