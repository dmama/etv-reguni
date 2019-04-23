package ch.vd.unireg.webservices.party3.data;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.webservices.party3.EnumTest;
import ch.vd.unireg.webservices.party3.impl.EnumHelper;
import ch.vd.unireg.xml.party.debtortype.v1.DebtorCategory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DebtorCategoryTest extends EnumTest {

	@Test
	public void testCoherence() {
		final Set<DebtorCategory> dcSet = EnumSet.allOf(DebtorCategory.class);
		final Set<CategorieImpotSource> cisSet = EnumSet.complementOf(EnumSet.of(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, CategorieImpotSource.EFFEUILLEUSES));
		assertEquals(dcSet.size(), cisSet.size());
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((CategorieImpotSource) null));
		assertEquals(DebtorCategory.ADMINISTRATORS, EnumHelper.coreToWeb(CategorieImpotSource.ADMINISTRATEURS));
		assertEquals(DebtorCategory.SPEAKERS_ARTISTS_SPORTSMEN, EnumHelper.coreToWeb(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS));
		assertEquals(DebtorCategory.MORTGAGE_CREDITORS, EnumHelper.coreToWeb(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES));
		assertEquals(DebtorCategory.PENSION_FUND, EnumHelper.coreToWeb(CategorieImpotSource.PRESTATIONS_PREVOYANCE));
		assertEquals(DebtorCategory.REGULAR, EnumHelper.coreToWeb(CategorieImpotSource.REGULIERS));
		assertEquals(DebtorCategory.LAW_ON_UNDECLARED_WORK, EnumHelper.coreToWeb(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR));

		try {
			EnumHelper.coreToWeb(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getMessage());
		}
		try {
			EnumHelper.coreToWeb(CategorieImpotSource.EFFEUILLEUSES);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Type de catégorie impôt source non supporté dans cette version du service", e.getMessage());
		}
	}
}
