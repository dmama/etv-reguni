package ch.vd.unireg.tiers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.tiers.Contribuable.FirstForsList;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ContribuableTest extends WithoutSpringTest {

	private ForFiscalAutreImpot newForChien(RegDate dateDebut, RegDate dateFin) {
		final ForFiscalAutreImpot for0 = new ForFiscalAutreImpot();
		for0.setDateDebut(dateDebut);
		for0.setDateFin(dateFin);
		for0.setGenreImpot(GenreImpot.CHIENS);
		for0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for0.setNumeroOfsAutoriteFiscale(1234);
		return for0;
	}

	private PersonnePhysique createHabitantWithFors() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(100011010L);
		hab.setNumeroIndividu(43L);

		Set<ForFiscal> fors = new HashSet<>();
		{
			ForFiscalAutreImpot forFiscal = new ForFiscalAutreImpot();
			forFiscal.setGenreImpot(GenreImpot.DROIT_MUTATION);
			forFiscal.setDateDebut(RegDate.get(2004, 3, 1));
			forFiscal.setDateFin(RegDate.get(2006, 2, 28));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1235);
			fors.add(forFiscal);
		}
		{
			ForFiscalAutreElementImposable forFiscal = new ForFiscalAutreElementImposable();
			forFiscal.setMotifRattachement(MotifRattachement.ACTIVITE_LUCRATIVE_CAS);
			forFiscal.setDateDebut(RegDate.get(2006, 6, 1));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1236);
			forFiscal.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			fors.add(forFiscal);
		}
		{
			ForFiscalSecondaire forFiscal = new ForFiscalSecondaire();
			forFiscal.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
			forFiscal.setDateDebut(RegDate.get(2002, 6, 1));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1237);
			forFiscal.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			fors.add(forFiscal);
		}

		// Principaux
		// 2002, 1, 1 - 2005, 8, 11
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2002, 1, 1));
			forFiscal.setDateFin(RegDate.get(2005, 8, 11));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			forFiscal.setMotifFermeture(MotifFor.DEPART_HS);

			fors.add(forFiscal);
		}
		// Annule : 2004, 6, 6 - 2005, 9, 9
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setAnnule(true);
			forFiscal.setDateDebut(RegDate.get(2004, 6, 6));
			forFiscal.setDateFin(RegDate.get(2005, 9, 9));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1563);
			fors.add(forFiscal);
		}
		// 2005, 8, 12 - 2007, 2, 28
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
			forFiscal.setDateFin(RegDate.get(2007, 2, 28));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setNumeroOfsAutoriteFiscale(1234);
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HS);
			fors.add(forFiscal);
		}
		// 2007, 3, 1 -> 2007, 3, 1 (1 jour)
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 1));
			forFiscal.setDateFin(RegDate.get(2007, 3, 1));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			fors.add(forFiscal);
		}
		// 2007, 3, 2 -> Ouvert
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 2));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			fors.add(forFiscal);
		}
		hab.setForsFiscaux(fors);
		return hab;
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

	@Test
	public void testDateDesactivation() throws Exception {

		// pas de for
		final PersonnePhysique pp = new PersonnePhysique(true);
		assertNull(pp.getDateDesactivation());

		// un for ouvert
		final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		pp.addForFiscal(ffp);
		assertNull(pp.getDateDesactivation());

		// fermeture du for principal pour motif annulation
		final RegDate dateDesactivation = date(2005, 5, 12);
		ffp.setMotifFermeture(MotifFor.ANNULATION);
		ffp.setDateFin(dateDesactivation);
		assertEquals(dateDesactivation, pp.getDateDesactivation());

		// ouverture d'un autre for plus tard
		final RegDate dateReactivation = dateDesactivation.addYears(1);
		final ForFiscalPrincipal nouveauFfp = new ForFiscalPrincipalPP(dateReactivation, MotifFor.REACTIVATION, null, null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
		pp.addForFiscal(nouveauFfp);
		assertNull(pp.getDateDesactivation());

		// et refermeture du for
		final RegDate dateReDesactivation = dateReactivation.addMonths(7);
		nouveauFfp.setDateFin(dateReDesactivation);
		nouveauFfp.setMotifFermeture(MotifFor.ANNULATION);
		assertEquals(dateReDesactivation, pp.getDateDesactivation());

		// ré-ouverture
		final RegDate dateDeuxiemeReactivation = dateReDesactivation.addYears(1);
		final ForFiscalPrincipal dernierFfp = new ForFiscalPrincipalPP(dateDeuxiemeReactivation, MotifFor.REACTIVATION, null, null, 1245, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
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

	private void assertForFiscauxInSortOrder(List<?> fors) {

		RegDate lastDate = null;
		for (Object o : fors) {
			ForFiscal ff = (ForFiscal) o;
			// String debut = ff.getDateDebut() != null ? ff.getDateDebut().toString() : "<null>";
			// String fin = ff.getDateFin() != null ? ff.getDateFin().toString() : "<null>";
			// String last = lastDate != null ? lastDate.toString() : "<null>";
			assertTrue("debut=" + ff.getDateDebut() + " last=" + lastDate, lastDate == null || ff.getDateDebut() == null
					|| ff.getDateDebut().isAfter(lastDate));
			lastDate = ff.getDateFin();
		}
	}

	@Test
	public void testGetForsFiscauxPrincipaux() {
		PersonnePhysique hab = createHabitantWithFors();

		List<ForFiscalPrincipalPP> ffps = hab.getForsFiscauxPrincipauxActifsSorted();
		assertEquals(4, ffps.size());
		assertForFiscauxInSortOrder(ffps);
	}

	@Test
	public void testGetForFiscalPrincipalAt() {
		PersonnePhysique hab = createHabitantWithFors();

		{
			ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(RegDate.get(2001, 8, 11));
			assertNull(ffp);
		}
		{
			ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(RegDate.get(2006, 12, 20));
			assertNotNull(ffp);
			assertEquals(new Integer(1234), ffp.getNumeroOfsAutoriteFiscale());
		}
		{
			ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(RegDate.get(2008, 3, 1));
			assertNotNull(ffp);
			assertEquals(new Integer(563), ffp.getNumeroOfsAutoriteFiscale());
		}
	}

	/**
	 * Collection vide
	 */
	@Test
	public void testExistForPrincipalListVide() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2005, 1, 1)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(2005, 1, 1)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
	}

	/**
	 * 1 for principal [2000-1-1; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurUnFor() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 1 for principal ouvert à gauche ]null; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurUnForOuvertAGauche() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(null);
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		assertTrue(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 1 for principal ouvert à droite [2000-1-1; null[
	 */
	@Test
	public void testExistForPrincipalSurUnForOuvertADroite() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 1 for principal ouvert des deux côtés ]null; null[
	 */
	@Test
	public void testExistForPrincipalSurUnForOuvertDesDeuxCotes() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(null);
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertTrue(Contribuable.existForPrincipal(list, null, null));
		assertTrue(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux accolés [2000-1-1; 2002-12-31] + [2003-1-1; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsAccoles() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2003, 1, 1));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux accolés ouverts à gauche ]null; 2002-12-31] + [2003-1-1; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsAccolesOuvertsAGauche() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(null);
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2003, 1, 1));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		assertTrue(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux accolés ouverts à droite [2000-1-1; 2002-12-31] + [2003-1-1; null[
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsAccolesOuvertsADroite() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2003, 1, 1));
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux non-accolés [2000-1-1; 2002-12-31] + [2003-1-2; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsNonAccoles() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2003, 1, 2));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2002, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2004, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux non-accolés ouverts à gauche ]null; 2002-12-31] + [2003-1-2; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsNonAccolesOuvertsAGauche() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(null);
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2003, 1, 2));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		assertTrue(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2002, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2004, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux non-accolés ouverts à droite [2000-1-1; 2002-12-31] + [2003-1-2; null[
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsNonAccolesOvertsADroite() {
		List<ForFiscalPrincipal> list = new ArrayList<>();
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(RegDate.get(2003, 1, 2));
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, null));
		Assert.assertFalse(Contribuable.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2002, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2004, 12, 31)));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2010, 12, 31)));
		Assert.assertFalse(Contribuable.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2003, 1, 2), null));
		assertTrue(Contribuable.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	@Test
	public void testGetDernierForFiscalPrincipal() {
		PersonnePhysique hab = createHabitantWithFors();
		ForFiscalPrincipal ffp = hab.getDernierForFiscalPrincipal();
		assertEquals(RegDate.get(2007, 3, 2), ffp.getDateDebut());
		assertNull(ffp.getDateFin());
	}
}
