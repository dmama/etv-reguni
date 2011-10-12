package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * Test qui vérifie que l'enum exposé dans les web-services est compatible avec celui utilisé en interne par Unireg.
 */
public class RelationBetweenPartiesTypeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(RelationBetweenPartiesType.class, ch.vd.uniregctb.type.TypeRapportEntreTiers.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeActivite) null));
		assertEquals(RelationBetweenPartiesType.GUARDIAN, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.TUTELLE));
		assertEquals(RelationBetweenPartiesType.WELFARE_ADVOCATE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CURATELLE));
		assertEquals(RelationBetweenPartiesType.LEGAL_ADVISER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(RelationBetweenPartiesType.TAXABLE_REVENUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(RelationBetweenPartiesType.HOUSEHOLD_MEMBER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(RelationBetweenPartiesType.REPRESENTATIVE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.REPRESENTATION));
		assertEquals(RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE));
		assertEquals(RelationBetweenPartiesType.CANCELS_AND_REPLACES, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.ANNULE_ET_REMPLACE));
	}
}
