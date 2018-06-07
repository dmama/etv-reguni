package ch.vd.unireg.security;

import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeDroitAcces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DroitAccesDAOTest extends CoreDAOTest {

	private DroitAccesDAO dao;

	@Before
	public void setUp() throws Exception {
		this.dao = getBean(DroitAccesDAO.class, "droitAccesDAO");
	}

	/**
	 * [SIFISC-29129] Vérifie que le visa opérateur n'est pas sensible à la casse sur la méthode getDroitAcces.
	 */
	@Test
	public void testGetDroitAccesCaseInsensitive() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jean", "Sairien", null, Sexe.MASCULIN);
			addDroitAcces("zaixxx", pp, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2000, 1, 1), null);
			addDroitAcces("zaiyyy", pp, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, date(2002, 1, 1), null);
			return pp.getNumero();
		});

		doInNewTransaction(status -> {
			assertDroitAcces(date(2000, 1, 1), null, "zaixxx", id, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, dao.getDroitAcces("zaixxx", id, RegDate.get()));
			assertDroitAcces(date(2000, 1, 1), null, "zaixxx", id, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, dao.getDroitAcces("ZAIXXX", id, RegDate.get()));
			assertDroitAcces(date(2002, 1, 1), null, "zaiyyy", id, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, dao.getDroitAcces("zaiyyy", id, RegDate.get()));
			assertDroitAcces(date(2002, 1, 1), null, "zaiyyy", id, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, dao.getDroitAcces("ZAIYYY", id, RegDate.get()));
			return null;
		});
	}

	/**
	 * [SIFISC-29129] Vérifie que le visa opérateur n'est pas sensible à la casse sur la méthode getDroitsAcces.
	 */
	@Test
	public void testGetDroitsAccesCaseInsensitive() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Jean", "Sairien", null, Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Moa", "Nonplus", null, Sexe.FEMININ);
			addDroitAcces("zaixxx", pp1, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2000, 1, 1), null);
			addDroitAcces("zaixxx", pp2, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, date(2002, 1, 1), null);
			ids.pp1 = pp1.getNumero();
			ids.pp2 = pp2.getNumero();
			return null;
		});

		// en minuscules
		doInNewTransaction(status -> {
			final List<DroitAcces> droitsAcces = dao.getDroitsAcces("zaixxx", null);
			assertNotNull(droitsAcces);
			assertEquals(2, droitsAcces.size());
			assertDroitAcces(date(2002, 1, 1), null, "zaixxx", ids.pp2, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, droitsAcces.get(0));
			assertDroitAcces(date(2000, 1, 1), null, "zaixxx", ids.pp1, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, droitsAcces.get(1));
			return null;
		});

		// en majuscules
		doInNewTransaction(status -> {
			final List<DroitAcces> droitsAcces = dao.getDroitsAcces("ZAIXXX", null);
			assertNotNull(droitsAcces);
			assertEquals(2, droitsAcces.size());
			assertDroitAcces(date(2002, 1, 1), null, "zaixxx", ids.pp2, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, droitsAcces.get(0));
			assertDroitAcces(date(2000, 1, 1), null, "zaixxx", ids.pp1, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, droitsAcces.get(1));
			return null;
		});
	}

	/**
	 * [SIFISC-29129] Vérifie que le visa opérateur n'est pas sensible à la casse sur la méthode getDroitAccesCount.
	 */
	@Test
	public void testGetDroitAccesCountCaseInsensitive() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Jean", "Sairien", null, Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Moa", "Nonplus", null, Sexe.FEMININ);
			addDroitAcces("zaixxx", pp1, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2000, 1, 1), null);
			addDroitAcces("zaixxx", pp2, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, date(2002, 1, 1), null);
			ids.pp1 = pp1.getNumero();
			ids.pp2 = pp2.getNumero();
			return null;
		});

		doInNewTransaction(status -> {
			assertEquals(Integer.valueOf(2), dao.getDroitAccesCount("zaixxx"));
			assertEquals(Integer.valueOf(2), dao.getDroitAccesCount("ZAIXXX"));
			return null;
		});
	}

	/**
	 * [SIFISC-29129] Vérifie que le visa opérateur n'est pas sensible à la casse sur la méthode getIdsDroitsAcces.
	 */
	@Test
	public void testGetIdsDroitsAccesCaseInsensitive() throws Exception {

		class Ids {
			Long da1;
			Long da2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Jean", "Sairien", null, Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Moa", "Nonplus", null, Sexe.FEMININ);
			final DroitAcces da1 = addDroitAcces("zaixxx", pp1, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2000, 1, 1), null);
			final DroitAcces da2 = addDroitAcces("zaixxx", pp2, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, date(2002, 1, 1), null);
			ids.da1 = da1.getId();
			ids.da2 = da2.getId();
			return null;
		});

		// en minuscules
		doInNewTransaction(status -> {
			final List<Long> list = dao.getIdsDroitsAcces("zaixxx");
			assertNotNull(list);
			assertEquals(2, list.size());
			list.sort(Comparator.naturalOrder());
			assertEquals(ids.da1, list.get(0));
			assertEquals(ids.da2, list.get(1));
			return null;
		});

		// en majuscules
		doInNewTransaction(status -> {
			final List<Long> list = dao.getIdsDroitsAcces("ZAIXXX");
			assertNotNull(list);
			assertEquals(2, list.size());
			list.sort(Comparator.naturalOrder());
			assertEquals(ids.da1, list.get(0));
			assertEquals(ids.da2, list.get(1));
			return null;
		});
	}

	private static void assertDroitAcces(RegDate dateDebut, Object dateFin, String operateur, Long tiersId, TypeDroitAcces type, Niveau niveau, DroitAcces droit) {
		assertNotNull(droit);
		assertEquals(dateDebut, droit.getDateDebut());
		assertEquals(dateFin, droit.getDateFin());
		assertEquals(niveau, droit.getNiveau());
		assertEquals(type, droit.getType());
		assertEquals(operateur, droit.getVisaOperateur());
		assertEquals(tiersId, droit.getTiers().getNumero());
	}
}