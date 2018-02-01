package ch.vd.unireg.webservices.party4.data;

import org.junit.Test;

import ch.vd.unireg.webservices.party4.SearchMode;
import ch.vd.unireg.webservices.party4.EnumTest;
import ch.vd.unireg.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class SearchModeTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(SearchMode.class, ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.TypeDocument) null));
		assertEquals(SearchMode.CONTAINS, EnumHelper.coreToWeb(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.CONTIENT));
		assertEquals(SearchMode.PHONETIC, EnumHelper.coreToWeb(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.PHONETIQUE));
		assertEquals(SearchMode.IS_EXACTLY, EnumHelper.coreToWeb(ch.vd.unireg.tiers.TiersCriteria.TypeRecherche.EST_EXACTEMENT));
	}
}
