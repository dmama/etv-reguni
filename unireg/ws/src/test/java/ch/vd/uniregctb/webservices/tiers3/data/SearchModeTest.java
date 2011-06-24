package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.SearchMode;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class SearchModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(SearchMode.class, ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeDocument) null));
		assertEquals(SearchMode.CONTAINS, EnumHelper.coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.CONTIENT));
		assertEquals(SearchMode.PHONETIC, EnumHelper.coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.PHONETIQUE));
		assertEquals(SearchMode.IS_EXACTLY, EnumHelper.coreToWeb(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.EST_EXACTEMENT));
	}
}
