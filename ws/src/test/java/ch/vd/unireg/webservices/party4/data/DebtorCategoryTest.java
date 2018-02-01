package ch.vd.unireg.webservices.party4.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.debtor.v2.DebtorCategory;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.webservices.party4.EnumTest;
import ch.vd.unireg.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DebtorCategoryTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(DebtorCategory.class, CategorieImpotSource.class);
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
		assertEquals(DebtorCategory.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYERS, EnumHelper.coreToWeb(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE));
		assertEquals(DebtorCategory.SEASONAL_WORKERS, EnumHelper.coreToWeb(CategorieImpotSource.EFFEUILLEUSES));
	}
}
