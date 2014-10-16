package ch.vd.uniregctb.webservices.v5;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class SearchModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(SearchMode.class, ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.toCore((SearchMode) null));
		assertEquals(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.CONTIENT, EnumHelper.toCore(SearchMode.CONTAINS));
		assertEquals(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.PHONETIQUE, EnumHelper.toCore(SearchMode.PHONETIC));
		assertEquals(ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche.EST_EXACTEMENT, EnumHelper.toCore(SearchMode.IS_EXACTLY));
	}
}
