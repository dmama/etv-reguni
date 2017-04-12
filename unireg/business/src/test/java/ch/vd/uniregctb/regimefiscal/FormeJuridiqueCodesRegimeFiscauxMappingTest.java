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

		assertEquals("00", getDefaultCodePourFormeJuridique(null));

		assertEquals("00", getDefaultCodePourFormeJuridique(EI));

		assertEquals("80", getDefaultCodePourFormeJuridique(SNC));
		assertEquals("80", getDefaultCodePourFormeJuridique(SC));

		assertEquals("01", getDefaultCodePourFormeJuridique(SCA));
		assertEquals("01", getDefaultCodePourFormeJuridique(SA));
		assertEquals("01", getDefaultCodePourFormeJuridique(SARL));
		assertEquals("01", getDefaultCodePourFormeJuridique(SCOOP));

		assertEquals("70", getDefaultCodePourFormeJuridique(ASSOCIATION));
		assertEquals("70", getDefaultCodePourFormeJuridique(FONDATION));

		assertEquals("00", getDefaultCodePourFormeJuridique(FILIALE_HS_RC));
		assertEquals("00", getDefaultCodePourFormeJuridique(PARTICULIER));
		assertEquals("00", getDefaultCodePourFormeJuridique(SCPC));
		assertEquals("00", getDefaultCodePourFormeJuridique(SICAV));
		assertEquals("00", getDefaultCodePourFormeJuridique(SICAF));
		assertEquals("00", getDefaultCodePourFormeJuridique(IDP));
		assertEquals("00", getDefaultCodePourFormeJuridique(PNC));
		assertEquals("00", getDefaultCodePourFormeJuridique(INDIVISION));

		assertEquals("01", getDefaultCodePourFormeJuridique(FILIALE_CH_RC));

		assertEquals("00", getDefaultCodePourFormeJuridique(ADM_CH));
		assertEquals("00", getDefaultCodePourFormeJuridique(ADM_CT));
		assertEquals("00", getDefaultCodePourFormeJuridique(ADM_DI));
		assertEquals("00", getDefaultCodePourFormeJuridique(ADM_CO));
		assertEquals("00", getDefaultCodePourFormeJuridique(CORP_DP_ADM));
		assertEquals("00", getDefaultCodePourFormeJuridique(ENT_CH));
		assertEquals("00", getDefaultCodePourFormeJuridique(ENT_CT));
		assertEquals("00", getDefaultCodePourFormeJuridique(ENT_DI));
		assertEquals("00", getDefaultCodePourFormeJuridique(ENT_CO));
		assertEquals("00", getDefaultCodePourFormeJuridique(CORP_DP_ENT));
		assertEquals("00", getDefaultCodePourFormeJuridique(SS));

		assertEquals("01", getDefaultCodePourFormeJuridique(FILIALE_HS_NIRC));

		assertEquals("00", getDefaultCodePourFormeJuridique(ENT_PUBLIQUE_HS));
		assertEquals("00", getDefaultCodePourFormeJuridique(ADM_PUBLIQUE_HS));
		assertEquals("00", getDefaultCodePourFormeJuridique(ORG_INTERNAT));
		assertEquals("00", getDefaultCodePourFormeJuridique(ENT_HS));
	}
}