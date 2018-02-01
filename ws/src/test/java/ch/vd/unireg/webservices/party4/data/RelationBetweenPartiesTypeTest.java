package ch.vd.unireg.webservices.party4.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.webservices.party4.EnumTest;
import ch.vd.unireg.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;

/**
 * Test qui vérifie que l'enum exposé dans les web-services est compatible avec celui utilisé en interne par Unireg.
 */
public class RelationBetweenPartiesTypeTest extends EnumTest {

// Suite à l'ajout des enfants et des parents, les deux enums ne sont plus comparables sur la longueur (SIFISC-2588)
//	@Test
//	public void testCoherence() {
//		assertEnumLengthEquals(RelationBetweenPartiesType.class, ch.vd.unireg.type.TypeRapportEntreTiers.class);
//	}

	@Test
	public void testFromValue() {
		assertEquals(RelationBetweenPartiesType.GUARDIAN, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.TUTELLE));
		assertEquals(RelationBetweenPartiesType.WELFARE_ADVOCATE, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.CURATELLE));
		assertEquals(RelationBetweenPartiesType.LEGAL_ADVISER, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(RelationBetweenPartiesType.TAXABLE_REVENUE, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(RelationBetweenPartiesType.HOUSEHOLD_MEMBER, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(RelationBetweenPartiesType.REPRESENTATIVE, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.REPRESENTATION));
		assertEquals(RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE));
		assertEquals(RelationBetweenPartiesType.CANCELS_AND_REPLACES, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeRapportEntreTiers.ANNULE_ET_REMPLACE));
	}
}
