package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;

public class RapportEntreTiersTest extends EnumTest {

	// msi 25.5.2009 : volontairement désactivé dans l'attente de la décision d'exposer le type de rapport CONTACT_IMPOT_SOURCE
	@Ignore
	@Test
	public void testTypeCoherence() {
		assertEnumLengthEquals(RapportEntreTiers.Type.class, TypeRapportEntreTiers.class);
		assertEnumConstantsEqual(RapportEntreTiers.Type.class, TypeRapportEntreTiers.class);
	}

	@Test
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeRapportEntreTiers) null));
		assertEquals(RapportEntreTiers.Type.TUTELLE, EnumHelper.coreToWeb(TypeRapportEntreTiers.TUTELLE));
		assertEquals(RapportEntreTiers.Type.CURATELLE, EnumHelper.coreToWeb(TypeRapportEntreTiers.CURATELLE));
		assertEquals(RapportEntreTiers.Type.CONSEIL_LEGAL, EnumHelper.coreToWeb(TypeRapportEntreTiers.CONSEIL_LEGAL));
		assertEquals(RapportEntreTiers.Type.PRESTATION_IMPOSABLE, EnumHelper.coreToWeb(TypeRapportEntreTiers.PRESTATION_IMPOSABLE));
		assertEquals(RapportEntreTiers.Type.APPARTENANCE_MENAGE, EnumHelper.coreToWeb(TypeRapportEntreTiers.APPARTENANCE_MENAGE));
		assertEquals(RapportEntreTiers.Type.REPRESENTATION, EnumHelper.coreToWeb(TypeRapportEntreTiers.REPRESENTATION));
	}
}
