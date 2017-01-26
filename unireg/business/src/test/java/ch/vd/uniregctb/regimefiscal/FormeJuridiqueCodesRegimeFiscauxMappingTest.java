package ch.vd.uniregctb.regimefiscal;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static ch.vd.uniregctb.regimefiscal.FormeJuridiqueCodesRegimeFiscauxMapping.getDefaultCodePourFormeJuridique;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Raphaël Marmier, 2017-01-27, <raphael.marmier@vd.ch>
 */
public class FormeJuridiqueCodesRegimeFiscauxMappingTest extends WithoutSpringTest {
	@Test
	public void testGetDefaultCodePourFormeJuridique() throws Exception {

		assertEquals("INDET", getDefaultCodePourFormeJuridique(null));

		assertEquals("INDET", getDefaultCodePourFormeJuridique(EI));

		assertEquals("SP", getDefaultCodePourFormeJuridique(SNC));
		assertEquals("SP", getDefaultCodePourFormeJuridique(SC));

		assertEquals("01", getDefaultCodePourFormeJuridique(SCA));
		assertEquals("01", getDefaultCodePourFormeJuridique(SA));
		assertEquals("01", getDefaultCodePourFormeJuridique(SARL));
		assertEquals("01", getDefaultCodePourFormeJuridique(SCOOP));

		assertEquals("70", getDefaultCodePourFormeJuridique(ASSOCIATION));
		assertEquals("70", getDefaultCodePourFormeJuridique(FONDATION));

		assertEquals("INDET", getDefaultCodePourFormeJuridique(FILIALE_HS_RC));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(PARTICULIER));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(SCPC));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(SICAV));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(SICAF));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(IDP));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(PNC));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(INDIVISION));

		assertEquals("01", getDefaultCodePourFormeJuridique(FILIALE_CH_RC));

		assertEquals("INDET", getDefaultCodePourFormeJuridique(ADM_CH));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ADM_CT));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ADM_DI));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ADM_CO));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(CORP_DP_ADM));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ENT_CH));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ENT_CT));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ENT_DI));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ENT_CO));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(CORP_DP_ENT));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(SS));

		assertEquals("01", getDefaultCodePourFormeJuridique(FILIALE_HS_NIRC));

		assertEquals("INDET", getDefaultCodePourFormeJuridique(ENT_PUBLIQUE_HS));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ADM_PUBLIQUE_HS));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ORG_INTERNAT));
		assertEquals("INDET", getDefaultCodePourFormeJuridique(ENT_HS));
	}
}