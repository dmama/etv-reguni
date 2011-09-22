package ch.vd.uniregctb.webservices.tiers2.data;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.EnumTest;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * Test qui vérifie que l'enum exposé dans les web-services est compatible avec celui utilisé en interne par Unireg.
 */
public class TypeRapportEntreTiersTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(RapportEntreTiers.Type.class, ch.vd.uniregctb.type.TypeRapportEntreTiers.class);
		assertEnumConstantsEqual(RapportEntreTiers.Type.class, ch.vd.uniregctb.type.TypeRapportEntreTiers.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeActivite) null));
		assertEquals(RapportEntreTiers.Type.TUTELLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.TUTELLE));
		assertEquals(RapportEntreTiers.Type.CURATELLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CURATELLE));
		assertEquals(RapportEntreTiers.Type.CONSEIL_LEGAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(RapportEntreTiers.Type.PRESTATION_IMPOSABLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(RapportEntreTiers.Type.APPARTENANCE_MENAGE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(RapportEntreTiers.Type.REPRESENTATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.REPRESENTATION));
		assertEquals(RapportEntreTiers.Type.CONTACT_IMPOT_SOURCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.CONTACT_IMPOT_SOURCE));
		assertEquals(RapportEntreTiers.Type.ANNULE_ET_REMPLACE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeRapportEntreTiers.ANNULE_ET_REMPLACE));
	}
}
