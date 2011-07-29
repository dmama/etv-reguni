package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementForsIteratorTest extends WithoutSpringTest {

	private static final ForsAt empty = new ForsAt(null);

	@Test
	public void testEmptyFors() {
		PersonnePhysique pp = new PersonnePhysique(false);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertFalse(iter.hasNext());
	}

	@Test
	public void testUnForDejaOuvert() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2005, 1, 1), null);
		pp.addForFiscal(ffp);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertFalse(iter.hasNext());
	}

	@Test
	public void testUnForDejaFerme() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2005, 1, 1), date(2005, 12, 31));
		pp.addForFiscal(ffp);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertFalse(iter.hasNext());
	}

	@Test
	public void testUnForOuvertDansLAnnee() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2007, 1, 1), null);
		pp.addForFiscal(ffp);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement d'ouverture du for principal
		final EvenementFors event = iter.next();
		assertNotNull(event);
		assertEquals(date(2007, 1, 1), event.dateEvenement);
		assertForsAt(new ForsAt(ffp), event.ouverts);
		assertForsAt(empty, event.fermes);
		assertForsAt(new ForsAt(ffp), event.actifs);
		assertForsAt(empty, event.actifsVeille);
		assertForsAt(new ForsAt(ffp), event.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testUnForOuvertEtFermeDansLAnnee() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2007, 1, 1), date(2007, 8, 14));
		pp.addForFiscal(ffp);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement d'ouverture du for principal
		final EvenementFors event0 = iter.next();
		assertNotNull(event0);
		assertEquals(date(2007, 1, 1), event0.dateEvenement);
		assertForsAt(new ForsAt(ffp), event0.ouverts);
		assertForsAt(empty, event0.fermes);
		assertForsAt(new ForsAt(ffp), event0.actifs);
		assertForsAt(empty, event0.actifsVeille);
		assertForsAt(new ForsAt(ffp), event0.actifsLendemain);

		// événement de fermeture du for principal
		final EvenementFors event1 = iter.next();
		assertNotNull(event1);
		assertEquals(date(2007, 8, 14), event1.dateEvenement);
		assertForsAt(empty, event1.ouverts);
		assertForsAt(new ForsAt(ffp), event1.fermes);
		assertForsAt(new ForsAt(ffp), event1.actifs);
		assertForsAt(new ForsAt(ffp), event1.actifsVeille);
		assertForsAt(empty, event1.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testUnForOuvertDansLAnneeEtFermeApres() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2007, 1, 1), date(2008, 2, 25));
		pp.addForFiscal(ffp);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement d'ouverture du for principal
		final EvenementFors event0 = iter.next();
		assertNotNull(event0);
		assertEquals(date(2007, 1, 1), event0.dateEvenement);
		assertForsAt(new ForsAt(ffp), event0.ouverts);
		assertForsAt(empty, event0.fermes);
		assertForsAt(new ForsAt(ffp), event0.actifs);
		assertForsAt(empty, event0.actifsVeille);
		assertForsAt(new ForsAt(ffp), event0.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testFermetureFor31Decembre() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2003, 1, 1), date(2007, 12, 31));
		pp.addForFiscal(ffp);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement de fermeture du for principal
		final EvenementFors event0 = iter.next();
		assertNotNull(event0);
		assertEquals(date(2007, 12, 31), event0.dateEvenement);
		assertForsAt(empty, event0.ouverts);
		assertForsAt(new ForsAt(ffp), event0.fermes);
		assertForsAt(new ForsAt(ffp), event0.actifs);
		assertForsAt(new ForsAt(ffp), event0.actifsVeille);
		assertForsAt(empty, event0.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testPlusieursOuvertureEtFermetureForsPrincipauxDansLAnnee() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp0 = addForPrincipal(date(2007, 1, 1), date(2007, 2, 25));
		final ForFiscalPrincipal ffp1 = addForPrincipal(date(2007, 2, 26), date(2007, 5, 10));
		final ForFiscalPrincipal ffp2 = addForPrincipal(date(2007, 5, 11), date(2007, 10, 28));
		final ForFiscalPrincipal ffp3 = addForPrincipal(date(2007, 10, 29), null);
		pp.addForFiscal(ffp0);
		pp.addForFiscal(ffp1);
		pp.addForFiscal(ffp2);
		pp.addForFiscal(ffp3);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement d'ouverture du for principal 0
		final EvenementFors event0 = iter.next();
		assertNotNull(event0);
		assertEquals(date(2007, 1, 1), event0.dateEvenement);
		assertForsAt(new ForsAt(ffp0), event0.ouverts);
		assertForsAt(empty, event0.fermes);
		assertForsAt(new ForsAt(ffp0), event0.actifs);
		assertForsAt(empty, event0.actifsVeille);
		assertForsAt(new ForsAt(ffp0), event0.actifsLendemain);

		// événement de fermeture du for principal 0
		final EvenementFors event1 = iter.next();
		assertNotNull(event1);
		assertEquals(date(2007, 2, 25), event1.dateEvenement);
		assertForsAt(empty, event1.ouverts);
		assertForsAt(new ForsAt(ffp0), event1.fermes);
		assertForsAt(new ForsAt(ffp0), event1.actifs);
		assertForsAt(new ForsAt(ffp0), event1.actifsVeille);
		assertForsAt(new ForsAt(ffp1), event1.actifsLendemain);

		// événement d'ouverture du for principal 1
		final EvenementFors event2 = iter.next();
		assertNotNull(event2);
		assertEquals(date(2007, 2, 26), event2.dateEvenement);
		assertForsAt(new ForsAt(ffp1), event2.ouverts);
		assertForsAt(empty, event2.fermes);
		assertForsAt(new ForsAt(ffp1), event2.actifs);
		assertForsAt(new ForsAt(ffp0), event2.actifsVeille);
		assertForsAt(new ForsAt(ffp1), event2.actifsLendemain);

		// événement de fermeture du for principal 1
		final EvenementFors event3 = iter.next();
		assertNotNull(event3);
		assertEquals(date(2007, 5, 10), event3.dateEvenement);
		assertForsAt(empty, event3.ouverts);
		assertForsAt(new ForsAt(ffp1), event3.fermes);
		assertForsAt(new ForsAt(ffp1), event3.actifs);
		assertForsAt(new ForsAt(ffp1), event3.actifsVeille);
		assertForsAt(new ForsAt(ffp2), event3.actifsLendemain);

		// événement d'ouverture du for principal 2
		final EvenementFors event4 = iter.next();
		assertNotNull(event4);
		assertEquals(date(2007, 5, 11), event4.dateEvenement);
		assertForsAt(new ForsAt(ffp2), event4.ouverts);
		assertForsAt(empty, event4.fermes);
		assertForsAt(new ForsAt(ffp2), event4.actifs);
		assertForsAt(new ForsAt(ffp1), event4.actifsVeille);
		assertForsAt(new ForsAt(ffp2), event4.actifsLendemain);

		// événement de fermeture du for principal 2
		final EvenementFors event5 = iter.next();
		assertNotNull(event5);
		assertEquals(date(2007, 10, 28), event5.dateEvenement);
		assertForsAt(empty, event5.ouverts);
		assertForsAt(new ForsAt(ffp2), event5.fermes);
		assertForsAt(new ForsAt(ffp2), event5.actifs);
		assertForsAt(new ForsAt(ffp2), event5.actifsVeille);
		assertForsAt(new ForsAt(ffp3), event5.actifsLendemain);

		// événement d'ouverture du for principal 3
		final EvenementFors event6 = iter.next();
		assertNotNull(event6);
		assertEquals(date(2007, 10, 29), event6.dateEvenement);
		assertForsAt(new ForsAt(ffp3), event6.ouverts);
		assertForsAt(empty, event6.fermes);
		assertForsAt(new ForsAt(ffp3), event6.actifs);
		assertForsAt(new ForsAt(ffp2), event6.actifsVeille);
		assertForsAt(new ForsAt(ffp3), event6.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testOuvertureEtFermetureDeForsPrincipauxEtSecondairesDansLAnnee() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2007, 1, 1), null);
		pp.addForFiscal(ffp);
		final ForFiscalSecondaire ffs0 = addForSecondaire(date(2007, 1, 1), date(2007, 9, 20));
		final ForFiscalSecondaire ffs1 = addForSecondaire(date(2007, 3, 15), date(2007, 12, 1));
		pp.addForFiscal(ffs0);
		pp.addForFiscal(ffs1);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement d'ouverture du for principal et du fors secondaire 0
		final EvenementFors event0 = iter.next();
		assertNotNull(event0);
		assertEquals(date(2007, 1, 1), event0.dateEvenement);
		assertForsAt(new ForsAt(ffp, ffs0), event0.ouverts);
		assertForsAt(empty, event0.fermes);
		assertForsAt(new ForsAt(ffp, ffs0), event0.actifs);
		assertForsAt(empty, event0.actifsVeille);
		assertForsAt(new ForsAt(ffp, ffs0), event0.actifsLendemain);

		// événement d'ouverture du du fors secondaire 1
		final EvenementFors event1 = iter.next();
		assertNotNull(event1);
		assertEquals(date(2007, 3, 15), event1.dateEvenement);
		assertForsAt(new ForsAt(null, ffs1), event1.ouverts);
		assertForsAt(empty, event1.fermes);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event1.actifs);
		assertForsAt(new ForsAt(ffp, ffs0), event1.actifsVeille);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event1.actifsLendemain);

		// événement de fermeture du for secondaire 0
		final EvenementFors event2 = iter.next();
		assertNotNull(event2);
		assertEquals(date(2007, 9, 20), event2.dateEvenement);
		assertForsAt(empty, event2.ouverts);
		assertForsAt(new ForsAt(null, ffs0), event2.fermes);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event2.actifs);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event2.actifsVeille);
		assertForsAt(new ForsAt(ffp, ffs1), event2.actifsLendemain);

		// événement de fermeture du for secondaire 1
		final EvenementFors event3 = iter.next();
		assertNotNull(event3);
		assertEquals(date(2007, 12, 1), event3.dateEvenement);
		assertForsAt(empty, event3.ouverts);
		assertForsAt(new ForsAt(null, ffs1), event3.fermes);
		assertForsAt(new ForsAt(ffp, ffs1), event3.actifs);
		assertForsAt(new ForsAt(ffp, ffs1), event3.actifsVeille);
		assertForsAt(new ForsAt(ffp), event3.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testOuvertureEtFermetureDeForsSecondairesEnMemeTemps() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final ForFiscalPrincipal ffp = addForPrincipal(date(2003, 1, 1), null);
		pp.addForFiscal(ffp);
		final ForFiscalSecondaire ffs0 = addForSecondaire(date(2007, 1, 1), date(2007, 9, 20));
		final ForFiscalSecondaire ffs1 = addForSecondaire(date(2007, 1, 1), date(2007, 4, 30));
		final ForFiscalSecondaire ffs2 = addForSecondaire(date(2007, 7, 1), date(2007, 9, 20));
		pp.addForFiscal(ffs0);
		pp.addForFiscal(ffs1);
		pp.addForFiscal(ffs2);

		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));
		assertTrue(iter.hasNext());

		// événement d'ouverture des fors secondaires 0 et 1
		final EvenementFors event0 = iter.next();
		assertNotNull(event0);
		assertEquals(date(2007, 1, 1), event0.dateEvenement);
		assertForsAt(new ForsAt(null, ffs0, ffs1), event0.ouverts);
		assertForsAt(empty, event0.fermes);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event0.actifs);
		assertForsAt(new ForsAt(ffp), event0.actifsVeille);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event0.actifsLendemain);

		// événement de fermeture du for secondaire 1
		final EvenementFors event1 = iter.next();
		assertNotNull(event1);
		assertEquals(date(2007, 4, 30), event1.dateEvenement);
		assertForsAt(empty, event1.ouverts);
		assertForsAt(new ForsAt(null, ffs1), event1.fermes);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event1.actifs);
		assertForsAt(new ForsAt(ffp, ffs0, ffs1), event1.actifsVeille);
		assertForsAt(new ForsAt(ffp, ffs0), event1.actifsLendemain);

		// événement d'ouverture du for secondaire 2
		final EvenementFors event2 = iter.next();
		assertNotNull(event2);
		assertEquals(date(2007, 7, 1), event2.dateEvenement);
		assertForsAt(new ForsAt(null, ffs2), event2.ouverts);
		assertForsAt(empty, event2.fermes);
		assertForsAt(new ForsAt(ffp, ffs0, ffs2), event2.actifs);
		assertForsAt(new ForsAt(ffp, ffs0), event2.actifsVeille);
		assertForsAt(new ForsAt(ffp, ffs0, ffs2), event2.actifsLendemain);

		// événement de fermeture des fors secondaires 0 et 2
		final EvenementFors event3 = iter.next();
		assertNotNull(event3);
		assertEquals(date(2007, 9, 20), event3.dateEvenement);
		assertForsAt(empty, event3.ouverts);
		assertForsAt(new ForsAt(null, ffs0, ffs2), event3.fermes);
		assertForsAt(new ForsAt(ffp, ffs0, ffs2), event3.actifs);
		assertForsAt(new ForsAt(ffp, ffs0, ffs2), event3.actifsVeille);
		assertForsAt(new ForsAt(ffp), event3.actifsLendemain);

		assertFalse(iter.hasNext());
	}

	@Test
	public void testRemove() {

		PersonnePhysique pp = new PersonnePhysique(false);
		final EvenementForsIterator iter = new EvenementForsIterator(new DecompositionForsAnneeComplete(pp, 2007));

		try {
			iter.remove();
			fail();
		}
		catch (NotImplementedException e) {
			// ok
		}
	}

	private static ForFiscalSecondaire addForSecondaire(RegDate dateOuverture, RegDate dateFermeture) {
		return new ForFiscalSecondaire(dateOuverture, dateFermeture, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
	}

	private static ForFiscalPrincipal addForPrincipal(RegDate dateOuverture, @Nullable RegDate dateFermeture) {
		return new ForFiscalPrincipal(dateOuverture, dateFermeture, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
	}

	private static void assertForsAt(ForsAt expected, ForsAt actual) {
		if (expected == null && actual == null) {
			return;
		}
		assertTrue(expected != null && actual != null);
		assertEquals(expected.principal, actual.principal);
		assertListEquals(expected.secondaires, actual.secondaires);
		assertEquals(expected.count, actual.count);
	}

	private static <T extends DateRange> void assertListEquals(List<T> expected, List<T> actual) {
		if (expected == null && actual == null) {
			return;
		}
		assertTrue(expected != null && actual != null);
		assertEquals(expected.size(), actual.size());

		// on crée des copies des listes triées par ordre chronologique
		List<T> se = new ArrayList<T>(expected);
		List<T> sa = new ArrayList<T>(actual);
		final DateRangeComparator<T> comparator = new DateRangeComparator<T>();
		Collections.sort(se, comparator);
		Collections.sort(sa, comparator);

		for (int i = 0; i < se.size(); ++i) {
			assertEquals(se.get(i), sa.get(i));
		}
	}

	private static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}
}
