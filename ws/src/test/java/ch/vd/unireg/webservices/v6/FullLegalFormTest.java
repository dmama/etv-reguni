package ch.vd.unireg.webservices.v6;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v4.FullLegalForm;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class FullLegalFormTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(FullLegalForm.class, FormeJuridiqueEntreprise.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (FormeJuridiqueEntreprise forme : FormeJuridiqueEntreprise.values()) {
			assertNotNull(forme.name(), EnumHelper.coreToWeb(forme));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((FormeJuridiqueEntreprise) null));
		assertEquals(FullLegalForm.ASSOCIATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ASSOCIATION));
		assertEquals(FullLegalForm.COOPERATIVE_SOCIETY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SCOOP));
		assertEquals(FullLegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.CORP_DP_ENT));
		assertEquals(FullLegalForm.SOLE_PROPRIETORSHIP, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.EI));
		assertEquals(FullLegalForm.FOUNDATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FONDATION));
		assertEquals(FullLegalForm.JOINT_POSSESSION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.INDIVISION));
		assertEquals(FullLegalForm.LIMITED_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SA));
		assertEquals(FullLegalForm.LIMITED_LIABILITY_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SARL));
		assertEquals(FullLegalForm.LIMITED_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SC));
		assertEquals(FullLegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SCA));
		assertEquals(FullLegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SCPC));
		assertEquals(FullLegalForm.CLOSED_END_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SICAF));
		assertEquals(FullLegalForm.OPEN_ENDED_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SICAV));
		assertEquals(FullLegalForm.FEDERAL_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_CH));
		assertEquals(FullLegalForm.CANTONAL_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_CT));
		assertEquals(FullLegalForm.DISTRICT_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_DI));
		assertEquals(FullLegalForm.MUNICIPALITY_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_CO));
		assertEquals(FullLegalForm.STATUTORY_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.CORP_DP_ADM));
		assertEquals(FullLegalForm.FEDERAL_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_CH));
		assertEquals(FullLegalForm.CANTONAL_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_CT));
		assertEquals(FullLegalForm.DISTRICT_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_DI));
		assertEquals(FullLegalForm.MUNICIPALITY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_CO));
		assertEquals(FullLegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.CORP_DP_ENT));
		assertEquals(FullLegalForm.FOREIGN_STATUTORY_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_PUBLIQUE_HS));
		assertEquals(FullLegalForm.FOREIGN_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_HS));
		assertEquals(FullLegalForm.FOREIGN_STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_PUBLIQUE_HS));
		assertEquals(FullLegalForm.UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FILIALE_HS_NIRC));
		assertEquals(FullLegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FILIALE_HS_RC));
		assertEquals(FullLegalForm.STATUTORY_INSTITUTE, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.IDP));
		assertEquals(FullLegalForm.BRANCH_OF_SWISS_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FILIALE_CH_RC));
		assertEquals(FullLegalForm.INTERNATIONAL_ORGANIZATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ORG_INTERNAT));
		assertEquals(FullLegalForm.OTHER, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.PARTICULIER));
		assertEquals(FullLegalForm.NON_COMMERCIAL_PROXY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.PNC));
		assertEquals(FullLegalForm.SIMPLE_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SS));
	}
}
