package ch.vd.unireg.webservices.v5;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class SearchModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(SearchMode.class, ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.toCore((SearchMode) null));
		assertEquals(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.CONTIENT, EnumHelper.toCore(SearchMode.CONTAINS));
		assertEquals(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.PHONETIQUE, EnumHelper.toCore(SearchMode.PHONETIC));
		assertEquals(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.EST_EXACTEMENT, EnumHelper.toCore(SearchMode.IS_EXACTLY));
	}
}
