package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.CategorieDebiteur;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class CategorieDebiteurTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(CategorieDebiteur.class, CategorieImpotSource.class);
		assertEnumConstantsEqual(CategorieDebiteur.class, CategorieImpotSource.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((CategorieImpotSource) null));
		assertEquals(CategorieDebiteur.ADMINISTRATEURS, EnumHelper.coreToWeb(CategorieImpotSource.ADMINISTRATEURS));
		assertEquals(CategorieDebiteur.CONFERENCIERS_ARTISTES_SPORTIFS, EnumHelper.coreToWeb(CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS));
		assertEquals(CategorieDebiteur.CREANCIERS_HYPOTHECAIRES, EnumHelper.coreToWeb(CategorieImpotSource.CREANCIERS_HYPOTHECAIRES));
		assertEquals(CategorieDebiteur.PRESTATIONS_PREVOYANCE, EnumHelper.coreToWeb(CategorieImpotSource.PRESTATIONS_PREVOYANCE));
		assertEquals(CategorieDebiteur.REGULIERS, EnumHelper.coreToWeb(CategorieImpotSource.REGULIERS));
		assertEquals(CategorieDebiteur.LOI_TRAVAIL_AU_NOIR, EnumHelper.coreToWeb(CategorieImpotSource.LOI_TRAVAIL_AU_NOIR));
	}
}
