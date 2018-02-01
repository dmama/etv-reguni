package ch.vd.unireg.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v5.LegalForm;
import ch.vd.unireg.type.FormeJuridique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class LegalFormTest extends EnumTest {

	@Test
	public void testCoherenceWithFormeJuridiqueEntreprise() {
		assertEnumLengthEquals(LegalForm.class, FormeJuridiqueEntreprise.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (FormeJuridiqueEntreprise forme : FormeJuridiqueEntreprise.values()) {
			assertNotNull(forme.name(), EnumHelper.coreToWeb(forme));
		}
	}

	@Test
	public void testFromFormeJuridiqueEntreprise() {
		assertNull(EnumHelper.coreToWeb((FormeJuridiqueEntreprise) null));
		assertEquals(LegalForm.ASSOCIATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ASSOCIATION));
		assertEquals(LegalForm.COOPERATIVE_SOCIETY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SCOOP));
		assertEquals(LegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.CORP_DP_ENT));
		assertEquals(LegalForm.SOLE_PROPRIETORSHIP, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.EI));
		assertEquals(LegalForm.FOUNDATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FONDATION));
		assertEquals(LegalForm.JOINT_POSSESSION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.INDIVISION));
		assertEquals(LegalForm.LIMITED_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SA));
		assertEquals(LegalForm.LIMITED_LIABILITY_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SARL));
		assertEquals(LegalForm.LIMITED_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SC));
		assertEquals(LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SCA));
		assertEquals(LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SCPC));
		assertEquals(LegalForm.CLOSED_END_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SICAF));
		assertEquals(LegalForm.OPEN_ENDED_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SICAV));
		assertEquals(LegalForm.FEDERAL_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_CH));
		assertEquals(LegalForm.CANTONAL_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_CT));
		assertEquals(LegalForm.DISTRICT_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_DI));
		assertEquals(LegalForm.MUNICIPALITY_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_CO));
		assertEquals(LegalForm.STATUTORY_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.CORP_DP_ADM));
		assertEquals(LegalForm.FEDERAL_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_CH));
		assertEquals(LegalForm.CANTONAL_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_CT));
		assertEquals(LegalForm.DISTRICT_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_DI));
		assertEquals(LegalForm.MUNICIPALITY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_CO));
		assertEquals(LegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.CORP_DP_ENT));
		assertEquals(LegalForm.FOREIGN_STATUTORY_ADMINISTRATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ADM_PUBLIQUE_HS));
		assertEquals(LegalForm.FOREIGN_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_HS));
		assertEquals(LegalForm.FOREIGN_STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ENT_PUBLIQUE_HS));
		assertEquals(LegalForm.UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FILIALE_HS_NIRC));
		assertEquals(LegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FILIALE_HS_RC));
		assertEquals(LegalForm.STATUTORY_INSTITUTE, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.IDP));
		assertEquals(LegalForm.BRANCH_OF_SWISS_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.FILIALE_CH_RC));
		assertEquals(LegalForm.INTERNATIONAL_ORGANIZATION, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.ORG_INTERNAT));
		assertEquals(LegalForm.OTHER, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.PARTICULIER));
		assertEquals(LegalForm.NON_COMMERCIAL_PROXY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.PNC));
		assertEquals(LegalForm.SIMPLE_COMPANY, EnumHelper.coreToWeb(FormeJuridiqueEntreprise.SS));
	}

	@Test
	public void testCoherenceWithFormeJuridique() {
//		assertEnumLengthEquals(LegalForm.class, FormeJuridique.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (FormeJuridique forme : FormeJuridique.values()) {
			assertNotNull(forme.name(), EnumHelper.coreToWeb(forme));
		}
	}

	@Test
	public void testFromFormeJuridique() {
		assertNull(EnumHelper.coreToWeb((FormeJuridique) null));
		assertEquals(LegalForm.ASSOCIATION, EnumHelper.coreToWeb(FormeJuridique.ASS));
		assertEquals(LegalForm.COOPERATIVE_SOCIETY, EnumHelper.coreToWeb(FormeJuridique.COOP));
		assertEquals(LegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridique.EDP));
		assertEquals(LegalForm.SOLE_PROPRIETORSHIP, EnumHelper.coreToWeb(FormeJuridique.EI));
		assertEquals(LegalForm.FOUNDATION, EnumHelper.coreToWeb(FormeJuridique.FOND));
		assertEquals(LegalForm.JOINT_POSSESSION, EnumHelper.coreToWeb(FormeJuridique.IND));
		assertEquals(LegalForm.OTHER, EnumHelper.coreToWeb(FormeJuridique.PRO));
		assertEquals(LegalForm.LIMITED_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SA));
		assertEquals(LegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridique.SAEDP));
		assertEquals(LegalForm.LIMITED_LIABILITY_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SARL));
		assertEquals(LegalForm.LIMITED_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridique.SC));
		assertEquals(LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridique.SCA));
		assertEquals(LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS, EnumHelper.coreToWeb(FormeJuridique.SCPC));
		assertEquals(LegalForm.REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SEE));
		assertEquals(LegalForm.BRANCH_OF_SWISS_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SES));
		assertEquals(LegalForm.CLOSED_END_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridique.SICAF));
		assertEquals(LegalForm.OPEN_ENDED_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridique.SICAV));
		assertEquals(LegalForm.GENERAL_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridique.SNC));
	}
	
}
