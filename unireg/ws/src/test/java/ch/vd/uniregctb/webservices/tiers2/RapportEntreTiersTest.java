package ch.vd.uniregctb.webservices.tiers2;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers2.data.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class RapportEntreTiersTest extends EnumTest {

	// msi 25.5.2009 : volontairement désactivé dans l'attente de la décision d'exposer le type de rapport CONTACT_IMPOT_SOURCE
	@Ignore
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTypeCoherence() {
		assertEnumLengthEquals(RapportEntreTiers.Type.class, TypeRapportEntreTiers.class);
		assertEnumConstantsEqual(RapportEntreTiers.Type.class, TypeRapportEntreTiers.class);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
