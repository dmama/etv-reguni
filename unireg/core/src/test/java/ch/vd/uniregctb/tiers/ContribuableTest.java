package ch.vd.uniregctb.tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.Contribuable.FirstForsList;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ContribuableTest extends WithoutSpringTest {

	private ForFiscalAutreImpot newForChien(RegDate dateDebut, RegDate dateFin) {
		ForFiscalAutreImpot for0 = new ForFiscalAutreImpot();
		for0.setDateDebut(dateDebut);
		for0.setDateFin(dateFin);
		for0.setGenreImpot(GenreImpot.CHIENS);
		for0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for0.setNumeroOfsAutoriteFiscale(Integer.valueOf(1234));
		return for0;
	}

	@Test
	public void testFirstForsList() {

		FirstForsList list = new FirstForsList();
		assertEmpty(list);

		// for inséré parce que la liste est vide
		final ForFiscalAutreImpot for0 = newForChien(date(1990, 1, 1), null);
		assertTrue(list.checkFor(for0));
		assertEquals(1, list.size());
		assertSame(for0, list.get(0));

		// for ignoré parce que 1995 > 1990
		final ForFiscalAutreImpot for1 = newForChien(date(1995, 1, 1), null);
		assertFalse(list.checkFor(for1));
		assertEquals(1, list.size());
		assertSame(for0, list.get(0));

		// for inséré parce que 1990 == 1990
		final ForFiscalAutreImpot for2 = newForChien(date(1990, 1, 1), null);
		assertTrue(list.checkFor(for2));
		assertEquals(2, list.size());
		assertSame(for0, list.get(0));
		assertSame(for2, list.get(1));

		// liste vidée puis for inséré parce que 1974 < 1990
		final ForFiscalAutreImpot for3 = newForChien(date(1974, 1, 1), null);
		assertTrue(list.checkFor(for3));
		assertEquals(1, list.size());
		assertSame(for3, list.get(0));

		// liste vidée puis for inséré parce que null < 1974
		final ForFiscalAutreImpot for4 = newForChien(null, null);
		assertTrue(list.checkFor(for4));
		assertEquals(1, list.size());
		assertSame(for4, list.get(0));

		// for ignoré parce que 2000 > null
		final ForFiscalAutreImpot for5 = newForChien(date(2000, 1, 1), null);
		assertFalse(list.checkFor(for5));
		assertEquals(1, list.size());
		assertSame(for4, list.get(0));

		// for inséré parce que null == null
		final ForFiscalAutreImpot for6 = newForChien(null, null);
		assertTrue(list.checkFor(for6));
		assertEquals(2, list.size());
		assertSame(for4, list.get(0));
		assertSame(for6, list.get(1));
	}

	private RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}
}
