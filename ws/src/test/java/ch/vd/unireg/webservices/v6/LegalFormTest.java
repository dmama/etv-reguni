package ch.vd.unireg.webservices.v6;

import org.junit.Test;

import ch.vd.unireg.xml.party.othercomm.v2.LegalForm;
import ch.vd.unireg.type.FormeJuridique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class LegalFormTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(LegalForm.class, FormeJuridique.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (FormeJuridique forme : FormeJuridique.values()) {
			assertNotNull(forme.name(), EnumHelper.coreToWeb(forme));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((FormeJuridique) null));
		assertEquals(LegalForm.ASSOCIATION, EnumHelper.coreToWeb(FormeJuridique.ASS));
		assertEquals(LegalForm.COOPERATIVE_SOCIETY, EnumHelper.coreToWeb(FormeJuridique.COOP));
		assertEquals(LegalForm.STATUTORY_CORPORATION, EnumHelper.coreToWeb(FormeJuridique.EDP));
		assertEquals(LegalForm.SOLE_PROPRIETORSHIP, EnumHelper.coreToWeb(FormeJuridique.EI));
		assertEquals(LegalForm.FOUNDATION, EnumHelper.coreToWeb(FormeJuridique.FOND));
		assertEquals(LegalForm.JOINT_POSSESSION, EnumHelper.coreToWeb(FormeJuridique.IND));
		assertEquals(LegalForm.CORPORATION_WITHOUT_COMPULSORY_REGISTRATION, EnumHelper.coreToWeb(FormeJuridique.PRO));
		assertEquals(LegalForm.LIMITED_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SA));
		assertEquals(LegalForm.PUBLIC_LIMITED_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SAEDP));
		assertEquals(LegalForm.LIMITED_LIABILITY_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SARL));
		assertEquals(LegalForm.LIMITED_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridique.SC));
		assertEquals(LegalForm.LIMITED_JOINT_STOCK_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridique.SCA));
		assertEquals(LegalForm.LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS, EnumHelper.coreToWeb(FormeJuridique.SCPC));
		assertEquals(LegalForm.BRANCH_OF_FOREIGN_BASED_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SEE));
		assertEquals(LegalForm.BRANCH_OF_SWISS_COMPANY, EnumHelper.coreToWeb(FormeJuridique.SES));
		assertEquals(LegalForm.CLOSED_END_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridique.SICAF));
		assertEquals(LegalForm.OPEN_ENDED_INVESTMENT_TRUST, EnumHelper.coreToWeb(FormeJuridique.SICAV));
		assertEquals(LegalForm.GENERAL_PARTNERSHIP, EnumHelper.coreToWeb(FormeJuridique.SNC));
	}
}
