package ch.vd.uniregctb.webservices.tiers2;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.data.TypeRecherche;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;


public class TypeRechercheTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(TypeRecherche.class, ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.class);
		assertEnumConstantsEqual(TypeRecherche.class, ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeDocument) null));
		assertEquals(TypeRecherche.CONTIENT, EnumHelper.coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.CONTIENT));
		assertEquals(TypeRecherche.PHONETIQUE, EnumHelper.coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.PHONETIQUE));
		assertEquals(TypeRecherche.EST_EXACTEMENT, EnumHelper.coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.EST_EXACTEMENT));
	}
}
