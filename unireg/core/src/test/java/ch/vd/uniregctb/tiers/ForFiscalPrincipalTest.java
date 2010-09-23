package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertFalse;

public class ForFiscalPrincipalTest extends WithoutSpringTest {

	@Test
	public void testValidateForAnnule() {

		final ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();

		// For invalide (mode d'imposition incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setModeImposition(null);
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}
	}

}