package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers2.EnumTest;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;

/**
 * Test qui vérifie que l'enum exposé dans les web-services est compatible avec celui utilisé en interne par Unireg.
 */
public class TypeRapportEntreTiersTest extends EnumTest {

	@Test
	public void testCoherence() {
		// les rapports de filiation ne peuvent pas sortir par la v2 du web-service tiers
		final Set<TypeRapportEntreTiers> coreEnumSet = EnumSet.complementOf(EnumSet.of(TypeRapportEntreTiers.FILIATION));
		final TypeRapportEntreTiers[] coreEnums = coreEnumSet.toArray(new TypeRapportEntreTiers[coreEnumSet.size()]);
		final RapportEntreTiers.Type[] wsEnums = RapportEntreTiers.Type.values();

		assertEnumLengthEquals(wsEnums, coreEnums);
		assertEnumConstantsEqual(wsEnums, coreEnums);
	}

	@Test
	public void testFromValue() {
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
