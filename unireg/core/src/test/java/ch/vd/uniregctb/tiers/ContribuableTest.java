package ch.vd.uniregctb.tiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.Contribuable.FirstForsList;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
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

	@Test
	public void testDateDesactivation() throws Exception {

		// pas de for
		final PersonnePhysique pp = new PersonnePhysique(true);
		assertNull(pp.getDateDesactivation());

		// un for ouvert
		final ForFiscalPrincipal ffp = new ForFiscalPrincipal(date(2000, 1, 1), null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HS);
		pp.addForFiscal(ffp);
		assertNull(pp.getDateDesactivation());

		// fermeture du for principal pour motif annulation
		final RegDate dateDesactivation = date(2005, 5, 12);
		ffp.setMotifFermeture(MotifFor.ANNULATION);
		ffp.setDateFin(dateDesactivation);
		assertEquals(dateDesactivation, pp.getDateDesactivation());

		// ouverture d'un autre for plus tard
		final RegDate dateReactivation = dateDesactivation.addYears(1);
		final ForFiscalPrincipal nouveauFfp = new ForFiscalPrincipal(dateReactivation, null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		nouveauFfp.setMotifOuverture(MotifFor.REACTIVATION);
		pp.addForFiscal(nouveauFfp);
		assertNull(pp.getDateDesactivation());

		// et refermeture du for
		final RegDate dateReDesactivation = dateReactivation.addMonths(7);
		nouveauFfp.setDateFin(dateReDesactivation);
		nouveauFfp.setMotifFermeture(MotifFor.ANNULATION);
		assertEquals(dateReDesactivation, pp.getDateDesactivation());

		// ré-ouverture
		final RegDate dateDeuxiemeReactivation = dateReDesactivation.addYears(1);
		final ForFiscalPrincipal dernierFfp = new ForFiscalPrincipal(dateDeuxiemeReactivation, null, 1245, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		dernierFfp.setMotifOuverture(MotifFor.REACTIVATION);
		pp.addForFiscal(dernierFfp);
		assertNull(pp.getDateDesactivation());

		// et fermeture sans annulation
		dernierFfp.setMotifFermeture(MotifFor.DEPART_HS);
		dernierFfp.setDateFin(dateDeuxiemeReactivation.addMonths(6));
		assertNull(pp.getDateDesactivation());

		// annulation du dernier for -> l'annulation devrait redevenir active
		dernierFfp.setAnnule(true);
		assertEquals(dateReDesactivation, pp.getDateDesactivation());
	}

}
