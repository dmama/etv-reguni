package ch.vd.uniregctb.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType;
import ch.vd.uniregctb.type.TypeFlagEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CorporationFlagTypeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(CorporationFlagType.class, TypeFlagEntreprise.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (TypeFlagEntreprise s : TypeFlagEntreprise.values()) {
			assertNotNull(s.name(), EnumHelper.coreToWeb(s));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeFlagEntreprise) null));
		assertEquals(CorporationFlagType.ASSOCIATION_FOUNDATION_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.APM_SOC_IMM_SUBVENTIONNEE));
		assertEquals(CorporationFlagType.AUDIT, EnumHelper.coreToWeb(TypeFlagEntreprise.AUDIT));
		assertEquals(CorporationFlagType.EXPERTISE, EnumHelper.coreToWeb(TypeFlagEntreprise.EXPERTISE));
		assertEquals(CorporationFlagType.MINIMAL_TAX, EnumHelper.coreToWeb(TypeFlagEntreprise.IMIN));
		assertEquals(CorporationFlagType.PUBLIC_INTEREST, EnumHelper.coreToWeb(TypeFlagEntreprise.UTILITE_PUBLIQUE));
		assertEquals(CorporationFlagType.REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_ORDINAIRE));
		assertEquals(CorporationFlagType.SERVICE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_SERVICE));
		assertEquals(CorporationFlagType.SOCIAL_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_CARACTERE_SOCIAL));
		assertEquals(CorporationFlagType.SUBSIDIZED_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_SUBVENTIONNEE));
		assertEquals(CorporationFlagType.TENANT_SHAREHOLDERS_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES));
	}
}
