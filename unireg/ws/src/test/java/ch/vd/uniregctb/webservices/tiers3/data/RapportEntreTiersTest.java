package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class RapportEntreTiersTest extends EnumTest {

	@Test
	public void testTypeCoherence() {
		assertEnumLengthEquals(TypeRapportEntreTiers.class, ch.vd.uniregctb.type.TypeRapportEntreTiers.class);
		assertEnumConstantsEqual(TypeRapportEntreTiers.class, ch.vd.uniregctb.type.TypeRapportEntreTiers.class);
	}

	@Test
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeRapportEntreTiers) null));
		assertEquals(TypeRapportEntreTiers.TUTELLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.TUTELLE));
		assertEquals(TypeRapportEntreTiers.CURATELLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CURATELLE));
		assertEquals(TypeRapportEntreTiers.CONSEIL_LEGAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(TypeRapportEntreTiers.PRESTATION_IMPOSABLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(TypeRapportEntreTiers.APPARTENANCE_MENAGE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(TypeRapportEntreTiers.REPRESENTATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.REPRESENTATION));
		assertEquals(TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE));
	}
}
