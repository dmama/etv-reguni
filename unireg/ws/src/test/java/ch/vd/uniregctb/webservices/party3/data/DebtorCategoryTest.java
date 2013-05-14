package ch.vd.uniregctb.webservices.party3.data;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.debtor.v1.DebtorCategory;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

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
	}
}
