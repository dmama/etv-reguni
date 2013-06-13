package ch.vd.uniregctb.webservices.tiers2;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.webservices.tiers2.data.CategorieDebiteur;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

public class CategorieDebiteurTest extends EnumTest {

	@Test
	public void testCoherence() {
		final Set<CategorieImpotSource> cisSet = EnumSet.complementOf(EnumSet.of(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));
		final Set<CategorieDebiteur> cdSet = EnumSet.allOf(CategorieDebiteur.class);

		assertEquals(cisSet.size(), cdSet.size());
		final Iterator<CategorieImpotSource> cisIterator = cisSet.iterator();
		final Iterator<CategorieDebiteur> cdIterator = cdSet.iterator();
		while (cisIterator.hasNext() && cdIterator.hasNext()) {
			final CategorieImpotSource cis = cisIterator.next();
			final CategorieDebiteur cd = cdIterator.next();

			assertEquals(cis.name(), cd.name());
		}
		assertFalse(cisIterator.hasNext());
		assertFalse(cdIterator.hasNext());
	}

	@Test
	public void testFromValue() throws Exception {
		assertNull(EnumHelper.coreToWeb((CategorieImpotSource) null));
		assertEquals(CategorieDebiteur.ADMINISTRATEURS, EnumHelper.coreToWeb(CategorieImpotSource.ADMINISTRATEURS));
		assertEquals(CategorieDebiteur.CONFERENCIERS_ARTISTES_SPORTIFS, EnumHelper.coreToWeb(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS));
		assertEquals(CategorieDebiteur.CREANCIERS_HYPOTHECAIRES, EnumHelper.coreToWeb(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES));
		assertEquals(CategorieDebiteur.PRESTATIONS_PREVOYANCE, EnumHelper.coreToWeb(CategorieImpotSource.PRESTATIONS_PREVOYANCE));
		assertEquals(CategorieDebiteur.REGULIERS, EnumHelper.coreToWeb(CategorieImpotSource.REGULIERS));
		assertEquals(CategorieDebiteur.LOI_TRAVAIL_AU_NOIR, EnumHelper.coreToWeb(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR));

		try {
			EnumHelper.coreToWeb(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE);
			fail();
		}
		catch (BusinessException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getMessage());
		}
		try {
			EnumHelper.coreToWeb(CategorieImpotSource.EFFEUILLEUSES);
			fail();
		}
		catch (BusinessException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getMessage());
		}
	}
}
