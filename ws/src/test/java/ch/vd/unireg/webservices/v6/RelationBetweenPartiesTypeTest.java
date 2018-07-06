package ch.vd.unireg.webservices.v6;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.relation.v3.RelationBetweenPartiesType;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test qui vérifie que l'enum exposé dans les web-services est compatible avec celui utilisé en interne par Unireg.
 */
public class RelationBetweenPartiesTypeTest extends EnumTest {

	@Test
	public void testCoherence() {
		// Suite à l'ajout des enfants et des parents, les deux enums ne sont plus comparables sur la longueur (SIFISC-2588)
//		assertEnumLengthEquals(RelationBetweenPartiesType.class, ch.vd.unireg.type.TypeRapportEntreTiers.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		final Set<TypeRapportEntreTiers> notMapped = EnumSet.of(TypeRapportEntreTiers.PARENTE,
		                                                        TypeRapportEntreTiers.ASSUJETTISSEMENT_PAR_SUBSTITUTION,
		                                                        TypeRapportEntreTiers.MANDAT,
		                                                        TypeRapportEntreTiers.SCISSION_ENTREPRISE,
		                                                        TypeRapportEntreTiers.TRANSFERT_PATRIMOINE,
		                                                        TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE,
		                                                        TypeRapportEntreTiers.SOCIETE_DIRECTION,
		                                                        TypeRapportEntreTiers.HERITAGE,TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC);
		for (TypeRapportEntreTiers tret : TypeRapportEntreTiers.values()) {
			if (notMapped.contains(tret)) {
				try {
					EnumHelper.coreToWeb(tret);
					fail("Mapping devrait exploser pour " + tret);
				}
				catch (IllegalArgumentException e) {
					assertEquals("Erreur de mapping?", e.getMessage());
				}
			}
			else {
				assertNotNull(tret.name(), EnumHelper.coreToWeb(tret));
			}
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeRapportEntreTiers) null));
		assertEquals(RelationBetweenPartiesType.GUARDIAN, EnumHelper.coreToWeb(TypeRapportEntreTiers.TUTELLE));
		assertEquals(RelationBetweenPartiesType.WELFARE_ADVOCATE, EnumHelper.coreToWeb(TypeRapportEntreTiers.CURATELLE));
		assertEquals(RelationBetweenPartiesType.LEGAL_ADVISER, EnumHelper.coreToWeb(TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(RelationBetweenPartiesType.TAXABLE_REVENUE, EnumHelper.coreToWeb(TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(RelationBetweenPartiesType.HOUSEHOLD_MEMBER, EnumHelper.coreToWeb(TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(RelationBetweenPartiesType.REPRESENTATIVE, EnumHelper.coreToWeb(TypeRapportEntreTiers.REPRESENTATION));
		assertEquals(RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT, EnumHelper.coreToWeb(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE));
		assertEquals(RelationBetweenPartiesType.CANCELS_AND_REPLACES, EnumHelper.coreToWeb(TypeRapportEntreTiers.ANNULE_ET_REMPLACE));
		assertEquals(RelationBetweenPartiesType.MERGE, EnumHelper.coreToWeb(TypeRapportEntreTiers.FUSION_ENTREPRISES));
		assertEquals(RelationBetweenPartiesType.ECONOMIC_ACTIVITY, EnumHelper.coreToWeb(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE));
	}
}
