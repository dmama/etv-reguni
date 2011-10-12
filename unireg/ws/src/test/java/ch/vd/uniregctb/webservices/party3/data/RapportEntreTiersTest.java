package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class RapportEntreTiersTest extends EnumTest {

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTypeCoherence() {
		assertEnumLengthEquals(RelationBetweenPartiesType.class, ch.vd.uniregctb.type.TypeRapportEntreTiers.class);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeRapportEntreTiers) null));
		assertEquals(RelationBetweenPartiesType.GUARDIAN, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.TUTELLE));
		assertEquals(RelationBetweenPartiesType.WELFARE_ADVOCATE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CURATELLE));
		assertEquals(RelationBetweenPartiesType.LEGAL_ADVISER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(RelationBetweenPartiesType.TAXABLE_REVENUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(RelationBetweenPartiesType.HOUSEHOLD_MEMBER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(RelationBetweenPartiesType.REPRESENTATIVE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.REPRESENTATION));
		assertEquals(RelationBetweenPartiesType.WITHHOLDING_TAX_CONTACT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE));
	}
}
