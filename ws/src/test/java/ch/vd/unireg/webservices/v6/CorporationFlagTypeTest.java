package ch.vd.unireg.webservices.v6;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.corporation.v4.CorporationFlagType;
import ch.vd.unireg.type.TypeFlagEntreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CorporationFlagTypeTest extends EnumTest {

	private static final Set<TypeFlagEntreprise> NOT_EXPOSED = EnumSet.of(TypeFlagEntreprise.AUDIT,
	                                                                      TypeFlagEntreprise.EXPERTISE,
	                                                                      TypeFlagEntreprise.IMIN);

	@Test
	public void testCoherence() {
		final int coreSize = TypeFlagEntreprise.values().length;
		final int wsSize = CorporationFlagType.values().length;
		assertEquals(coreSize - NOT_EXPOSED.size(), wsSize);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (TypeFlagEntreprise s : TypeFlagEntreprise.values()) {
			if (NOT_EXPOSED.contains(s)) {
				try {
					EnumHelper.coreToWeb(s);
					fail("La constante " + s + " ne devrait pas être mappée...");
				}
				catch (IllegalArgumentException e) {
					assertEquals("Type de flag inconnu : " + s, e.getMessage());
				}
			}
			else {
				assertNotNull(s.name(), EnumHelper.coreToWeb(s));
			}
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeFlagEntreprise) null));
		assertEquals(CorporationFlagType.ASSOCIATION_FOUNDATION_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.APM_SOC_IMM_SUBVENTIONNEE));
		assertEquals(CorporationFlagType.PUBLIC_INTEREST, EnumHelper.coreToWeb(TypeFlagEntreprise.UTILITE_PUBLIQUE));
		assertEquals(CorporationFlagType.REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_ORDINAIRE));
		assertEquals(CorporationFlagType.SERVICE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_SERVICE));
		assertEquals(CorporationFlagType.SOCIAL_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_CARACTERE_SOCIAL));
		assertEquals(CorporationFlagType.SUBSIDIZED_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_SUBVENTIONNEE));
		assertEquals(CorporationFlagType.TENANT_SHAREHOLDERS_REAL_ESTATE_COMPANY, EnumHelper.coreToWeb(TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES));
	}
}
