package ch.vd.unireg.coordfin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.coordfin.CoordonneesFinancieresService.UpdateResult;
import ch.vd.unireg.hibernate.MockHibernateTemplate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.PersonnePhysique;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CoordonneesFinancieresServiceTest {

	private CoordonneesFinancieresService service;

	@Before
	public void setUp() throws Exception {
		CoordonneesFinancieresServiceImpl service = new CoordonneesFinancieresServiceImpl();
		service.setIbanValidator(new IbanValidator());
		this.service = service;
	}

	@Test
	public void testAddCoordonneesFinancieresTousElementsVides() {
		try {
			service.addCoordonneesFinancieres(null, RegDate.get(2000, 1, 1), null, null, "", "  ");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Tous les éléments sont vides", e.getMessage());
		}
	}

	@Test
	public void testAjoutIbanAvecIbanEnBddSansDateDebutNiDeFin() {
		try {
			// Cf. [SIFISC-29616] Crash à l'ajout d'un nouvel IBAN
			final PersonnePhysique pp = new PersonnePhysique();
			pp.addCoordonneesFinancieres(new CoordonneesFinancieres("ToutesDatesNullEnBase", "CH0000", "  "));
			// ajoute une  coordonnée ouvert sur 2018
			service.addCoordonneesFinancieres(pp, RegDate.get(2018, 1, 1), null, "titulaire", "CH9308440717427290198", "bicboc");
			{
				final List<CoordonneesFinancieres> coordonnees = new ArrayList<>(pp.getCoordonneesFinancieres());
				coordonnees.sort(Comparator.comparing(CoordonneesFinancieres::getDateFin, Comparator.nullsLast(Comparator.naturalOrder())));
				assertNotNull(coordonnees);
				assertEquals(2, coordonnees.size());

				final CoordonneesFinancieres coord1 = coordonnees.get(1);
				assertNotNull(coord1);
				assertEquals(RegDate.get(2018, 1, 1), coord1.getDateDebut());
				assertNull(coord1.getDateFin());
				assertEquals("titulaire", coord1.getTitulaire());
				final CompteBancaire compteBancaire = coord1.getCompteBancaire();
				assertNotNull(compteBancaire);
				assertEquals("CH9308440717427290198", compteBancaire.getIban());
				assertEquals("bicboc", compteBancaire.getBicSwift());

				//verifie que la coordonnée 2018 n'est pas cloturées.
				final CoordonneesFinancieres coord0 = coordonnees.get(0);
				assertNotNull(coord0.getDateFin());
				assertEquals("ToutesDatesNullEnBase", coord0.getTitulaire());
			}
		}
		catch (IllegalArgumentException e) {
			fail();
		}

	}

	@Test
	public void testAddCoordonneesFinancieresIbanInvalide() {

		final PersonnePhysique pp = new PersonnePhysique();
		try {
			service.addCoordonneesFinancieres(pp, RegDate.get(2000, 1, 1), null, null, "CH0000", "  ");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("L'iban spécifié [CH0000] n'est pas valide", e.getMessage());
		}
	}

	@Test
	public void testAddCoordonneesFinancieresHistoriqueOK() {
		final PersonnePhysique pp = new PersonnePhysique();
		// ajoute une  coordonnée ouvert sur 2018
		service.addCoordonneesFinancieres(pp, RegDate.get(2018, 1, 1), null, "titulaire", "CH9308440717427290198", "bicboc");
		{
			final Set<CoordonneesFinancieres> coordonnees = pp.getCoordonneesFinancieres();

			assertNotNull(coordonnees);
			assertEquals(1, coordonnees.size());

			final CoordonneesFinancieres coord0 = coordonnees.iterator().next();
			assertNotNull(coord0);
			assertEquals(RegDate.get(2018, 1, 1), coord0.getDateDebut());
			assertNull(coord0.getDateFin());
			assertEquals("titulaire", coord0.getTitulaire());
			final CompteBancaire compteBancaire = coord0.getCompteBancaire();
			assertNotNull(compteBancaire);
			assertEquals("CH9308440717427290198", compteBancaire.getIban());
			assertEquals("bicboc", compteBancaire.getBicSwift());
		}

		// ajoute une  coordonnée  sur 2017
		service.addCoordonneesFinancieres(pp, RegDate.get(2017, 1, 1), RegDate.get(2017, 12, 31), "titulaire", "CH9308440717427290198", "bicboc");
		{
			final List<CoordonneesFinancieres> coordonnees = new ArrayList<>(pp.getCoordonneesFinancieres());
			coordonnees.sort(Comparator.comparing(CoordonneesFinancieres::getDateDebut, RegDate::compareTo));
			assertNotNull(coordonnees);
			assertEquals(2, coordonnees.size());

			final CoordonneesFinancieres coord0 = coordonnees.get(0);
			assertNotNull(coord0);
			assertEquals(RegDate.get(2017, 1, 1), coord0.getDateDebut());
			assertEquals(RegDate.get(2017, 12, 31), coord0.getDateFin());
			assertEquals("titulaire", coord0.getTitulaire());
			final CompteBancaire compteBancaire = coord0.getCompteBancaire();
			assertNotNull(compteBancaire);
			assertEquals("CH9308440717427290198", compteBancaire.getIban());
			assertEquals("bicboc", compteBancaire.getBicSwift());
		}

		// ajoute une  coordonnée ouvert sur 2016.
		service.addCoordonneesFinancieres(pp, RegDate.get(2016, 1, 1), null, "titulaire", "CH9308440717427290198", "bicboc");
		{
			final List<CoordonneesFinancieres> coordonnees = new ArrayList<>(pp.getCoordonneesFinancieres());
			coordonnees.sort(Comparator.comparing(CoordonneesFinancieres::getDateDebut, RegDate::compareTo));
			assertNotNull(coordonnees);
			assertEquals(3, coordonnees.size());
			final CoordonneesFinancieres coord2 = coordonnees.get(0);
			assertNotNull(coord2);
			assertEquals(RegDate.get(2016, 1, 1), coord2.getDateDebut());
			assertNull(coord2.getDateFin());
			assertEquals("titulaire", coord2.getTitulaire());
			final CompteBancaire compteBancaire = coord2.getCompteBancaire();
			assertNotNull(compteBancaire);
			assertEquals("CH9308440717427290198", compteBancaire.getIban());
			assertEquals("bicboc", compteBancaire.getBicSwift());

			//verifie que la coordonnée 2018 n'est pas cloturées.
			final CoordonneesFinancieres coord0 = coordonnees.get(2);
			assertNull(coord0.getDateFin());
		}
	}

	@Test
	public void testAddCoordonneesFinancieresOK() {

		final PersonnePhysique pp = new PersonnePhysique();

		// ajoute de premières coordonnées
		service.addCoordonneesFinancieres(pp, RegDate.get(2000, 1, 1), null, "titulaire", "CH9308440717427290198", "bicboc");
		{
			final Set<CoordonneesFinancieres> coordonnees = pp.getCoordonneesFinancieres();
			assertNotNull(coordonnees);
			assertEquals(1, coordonnees.size());

			final CoordonneesFinancieres coord0 = coordonnees.iterator().next();
			assertNotNull(coord0);
			assertEquals(RegDate.get(2000, 1, 1), coord0.getDateDebut());
			assertNull(coord0.getDateFin());
			assertEquals("titulaire", coord0.getTitulaire());
			final CompteBancaire compteBancaire = coord0.getCompteBancaire();
			assertNotNull(compteBancaire);
			assertEquals("CH9308440717427290198", compteBancaire.getIban());
			assertEquals("bicboc", compteBancaire.getBicSwift());
		}

		// ajoute de secondes coordonnées
		service.addCoordonneesFinancieres(pp, RegDate.get(2013, 4, 21), null, "titulaire", "CH690023000123456789A", null);
		{
			final List<CoordonneesFinancieres> coordonnees = new ArrayList<>(pp.getCoordonneesFinancieres());
			assertNotNull(coordonnees);
			coordonnees.sort(DateRangeComparator::compareRanges);
			assertEquals(2, coordonnees.size());

			final CoordonneesFinancieres coord0 = coordonnees.get(0);
			assertNotNull(coord0);
			assertEquals(RegDate.get(2000, 1, 1), coord0.getDateDebut());
			assertEquals(RegDate.get(2013, 4, 20), coord0.getDateFin()); // <-- la date de fin est maintenant renseignée
			assertEquals("titulaire", coord0.getTitulaire());
			final CompteBancaire compteBancaire0 = coord0.getCompteBancaire();
			assertNotNull(compteBancaire0);
			assertEquals("CH9308440717427290198", compteBancaire0.getIban());
			assertEquals("bicboc", compteBancaire0.getBicSwift());

			final CoordonneesFinancieres coord1 = coordonnees.get(1);
			assertNotNull(coord1);
			assertEquals(RegDate.get(2013, 4, 21), coord1.getDateDebut());
			assertNull(coord1.getDateFin());
			assertEquals("titulaire", coord1.getTitulaire());
			final CompteBancaire compteBancaire1 = coord1.getCompteBancaire();
			assertNotNull(compteBancaire1);
			assertEquals("CH690023000123456789A", compteBancaire1.getIban());
			assertNull(compteBancaire1.getBicSwift());
		}
	}

	@Test
	public void testUpdateCoordonneesFinancieresTousElementsVides() {
		try {
			service.updateCoordonneesFinancieres(1, RegDate.get(2000, 1, 1), null, "", "  ");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Tous les éléments sont vides", e.getMessage());
		}
	}

	@Test
	public void testUpdateCoordonneesFinancieresIbanInvalide() {

		final PersonnePhysique pp = new PersonnePhysique();
		try {
			service.updateCoordonneesFinancieres(1, RegDate.get(2000, 1, 1), null, "CH0000", "  ");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("L'iban spécifié [CH0000] n'est pas valide", e.getMessage());
		}
	}

	/**
	 * Teste la méthode 'updateCoordonneesFinancieres'  dans le cas où une date de fin est renseignée.
	 */
	@Test
	public void testUpdateCoordonneesFinancieresAjoutDateDeFin() {

		final long id = 1L;

		final CoordonneesFinancieres coord = new CoordonneesFinancieres();
		coord.setId(id);
		coord.setDateDebut(RegDate.get(2000, 1, 1));
		coord.setDateFin(null);
		coord.setTitulaire("titulaire");

		final PersonnePhysique pp = new PersonnePhysique();
		pp.addCoordonneesFinancieres(coord);

		// petit hack pour éviter d'utiliser un vrai hibernate template
		((CoordonneesFinancieresServiceImpl) service).setHibernateTemplate(new MockHibernateTemplate() {
			@Override
			public <T> T get(Class<T> clazz, Serializable id) {
				return (T) coord;
			}
		});

		// on renseigne la date fin
		final UpdateResult result = service.updateCoordonneesFinancieres(id, RegDate.get(2004, 5, 15), "titulaire", null, null);
		assertEquals(UpdateResult.CLOSED, result);

		// il ne doit y avoir toujours qu'une seule coordonnée et la date de fin doit maintenant être renseignée
		final Set<CoordonneesFinancieres> coordonnees = pp.getCoordonneesFinancieres();
		assertNotNull(coordonnees);
		assertEquals(1, coordonnees.size());

		final CoordonneesFinancieres coord0 = coordonnees.iterator().next();
		assertNotNull(coord0);
		assertEquals(RegDate.get(2000, 1, 1), coord0.getDateDebut());
		assertEquals(RegDate.get(2004, 5, 15), coord0.getDateFin());
		assertEquals("titulaire", coord0.getTitulaire());
		final CompteBancaire compteBancaire = coord0.getCompteBancaire();
		assertNull(compteBancaire);
	}

	/**
	 * Teste la méthode 'updateCoordonneesFinancieres'  dans le cas où autre chose qu'une date de fin est renseignée.
	 */
	@Test
	public void testUpdateCoordonneesFinancieresModificationTitulaire() {

		final PersonnePhysique pp = new PersonnePhysique();

		final CoordonneesFinancieres coord1 = new CoordonneesFinancieres();
		coord1.setId(1L);
		coord1.setDateDebut(RegDate.get(2000, 1, 1));
		coord1.setDateFin(RegDate.get(2004, 12, 31));
		coord1.setTitulaire("titulaire1");
		pp.addCoordonneesFinancieres(coord1);

		final CoordonneesFinancieres coord2 = new CoordonneesFinancieres();
		coord2.setId(2L);
		coord2.setDateDebut(RegDate.get(2005, 1, 1));
		coord2.setDateFin(null);
		coord2.setTitulaire("titulaire2");
		pp.addCoordonneesFinancieres(coord2);

		// petit hack pour éviter d'utiliser un vrai hibernate template
		((CoordonneesFinancieresServiceImpl) service).setHibernateTemplate(new MockHibernateTemplate() {
			@Override
			public <T> T get(Class<T> clazz, Serializable id) {
				return (T) coord2;
			}
		});

		// on modifie le titulaire
		final UpdateResult result = service.updateCoordonneesFinancieres(2L, null, "nouveau titulaire", null, null);
		assertEquals(UpdateResult.UPDATED, result);

		// l'ancienne coordonnée doit être annulée et une nouvelle avec le nouveau titulaire créée
		final List<CoordonneesFinancieres> coordonnees = new ArrayList<>(pp.getCoordonneesFinancieres());
		assertNotNull(coordonnees);
		coordonnees.sort(new AnnulableHelper.AnnulableDateRangeComparator<>(false));
		assertEquals(3, coordonnees.size());

		final CoordonneesFinancieres c0 = coordonnees.get(0);
		assertNotNull(c0);
		assertFalse(c0.isAnnule());
		assertEquals(RegDate.get(2000, 1, 1), c0.getDateDebut());
		assertEquals(RegDate.get(2004, 12, 31), c0.getDateFin());
		assertEquals("titulaire1", c0.getTitulaire());
		assertNull(c0.getCompteBancaire());

		final CoordonneesFinancieres c1 = coordonnees.get(1);
		assertNotNull(c1);
		assertFalse(c1.isAnnule());
		assertEquals(RegDate.get(2005, 1, 1), c1.getDateDebut());
		assertNull(c1.getDateFin());
		assertEquals("nouveau titulaire", c1.getTitulaire());   // <--- le nouveau titulaire
		final CompteBancaire compteBancaire1 = c1.getCompteBancaire();
		assertNotNull(compteBancaire1);
		assertNull(compteBancaire1.getIban());
		assertNull(compteBancaire1.getBicSwift());

		final CoordonneesFinancieres c2 = coordonnees.get(2);
		assertNotNull(c2);
		assertTrue(c2.isAnnule());                              // <--- l'ancienne coordonnées maintenant annulée
		assertEquals(RegDate.get(2005, 1, 1), c2.getDateDebut());
		assertNull(c2.getDateFin());
		assertEquals("titulaire2", c2.getTitulaire());
		assertNull(c2.getCompteBancaire());
	}

	/**
	 * [SIFISC-30072] Teste la méthode 'updateCoordonneesFinancieres'  dans le cas où aucune modification n'est proposée.
	 */
	@Test
	public void testUpdateCoordonneesFinancieresAucuneModification() {

		final PersonnePhysique pp = new PersonnePhysique();

		final CoordonneesFinancieres coord1 = new CoordonneesFinancieres();
		coord1.setId(1L);
		coord1.setDateDebut(RegDate.get(2000, 1, 1));
		coord1.setDateFin(RegDate.get(2004, 12, 31));
		coord1.setTitulaire("titulaire1");
		pp.addCoordonneesFinancieres(coord1);

		final CoordonneesFinancieres coord2 = new CoordonneesFinancieres();
		coord2.setId(2L);
		coord2.setDateDebut(RegDate.get(2005, 1, 1));
		coord2.setDateFin(null);
		coord2.setTitulaire("titulaire2");
		pp.addCoordonneesFinancieres(coord2);

		// petit hack pour éviter d'utiliser un vrai hibernate template
		((CoordonneesFinancieresServiceImpl) service).setHibernateTemplate(new MockHibernateTemplate() {
			@Override
			public <T> T get(Class<T> clazz, Serializable id) {
				return (T) coord2;
			}
		});

		// on ne modifie rien
		final UpdateResult result = service.updateCoordonneesFinancieres(2L, null, "titulaire2", null, null);
		assertEquals(UpdateResult.NOOP, result);

		// il ne doit y avoir aucun changement
		final List<CoordonneesFinancieres> coordonnees = new ArrayList<>(pp.getCoordonneesFinancieres());
		assertNotNull(coordonnees);
		coordonnees.sort(new AnnulableHelper.AnnulableDateRangeComparator<>(false));
		assertEquals(2, coordonnees.size());

		final CoordonneesFinancieres c0 = coordonnees.get(0);
		assertNotNull(c0);
		assertFalse(c0.isAnnule());
		assertEquals(RegDate.get(2000, 1, 1), c0.getDateDebut());
		assertEquals(RegDate.get(2004, 12, 31), c0.getDateFin());
		assertEquals("titulaire1", c0.getTitulaire());
		assertNull(c0.getCompteBancaire());

		final CoordonneesFinancieres c1 = coordonnees.get(1);
		assertNotNull(c1);
		assertFalse(c1.isAnnule());
		assertEquals(RegDate.get(2005, 1, 1), c1.getDateDebut());
		assertNull(c1.getDateFin());
		assertEquals("titulaire2", c1.getTitulaire());
		assertNull(c1.getCompteBancaire());
	}

	@Test
	public void testCancelCoordonneesFinancieresDejaAnnule() {

		final long id = 1L;

		final CoordonneesFinancieres coord = new CoordonneesFinancieres();
		coord.setId(id);
		coord.setDateDebut(RegDate.get(2000, 1, 1));
		coord.setDateFin(null);
		coord.setTitulaire("titulaire");
		coord.setAnnule(true);

		final PersonnePhysique pp = new PersonnePhysique();
		pp.addCoordonneesFinancieres(coord);

		// petit hack pour éviter d'utiliser un vrai hibernate template
		((CoordonneesFinancieresServiceImpl) service).setHibernateTemplate(new MockHibernateTemplate() {
			@Override
			public <T> T get(Class<T> clazz, Serializable id) {
				return (T) coord;
			}
		});

		// on annule les coordonnées
		try {
			service.cancelCoordonneesFinancieres(id);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Les coordonnées avec l'id=[1] sont déjà annulées", e.getMessage());
		}
	}

	@Test
	public void testCancelCoordonneesFinancieresOK() {

		final long id = 1L;

		final CoordonneesFinancieres coord = new CoordonneesFinancieres();
		coord.setId(id);
		coord.setDateDebut(RegDate.get(2000, 1, 1));
		coord.setDateFin(null);
		coord.setTitulaire("titulaire");

		final PersonnePhysique pp = new PersonnePhysique();
		pp.addCoordonneesFinancieres(coord);

		// petit hack pour éviter d'utiliser un vrai hibernate template
		((CoordonneesFinancieresServiceImpl) service).setHibernateTemplate(new MockHibernateTemplate() {
			@Override
			public <T> T get(Class<T> clazz, Serializable id) {
				return (T) coord;
			}
		});

		// on annule les coordonnées
		service.cancelCoordonneesFinancieres(id);
		assertTrue(coord.isAnnule());
	}

	/**
	 * Ce test vérifie que l'ajout de coordonnées financières valides sur un contribuable vide fonctionne bien.
	 */
	@Test
	public void testDetectAndUpdateIbanValideSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH7400243243G15379860", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est valide"));

		// on vérifie que les coordonnées financières ont bien été ajoutées
		final Set<CoordonneesFinancieres> coordonneesFinancieres = ctb.getCoordonneesFinancieres();
		assertNotNull(coordonneesFinancieres);
		assertEquals(1, coordonneesFinancieres.size());
		assertCoordFin(RegDate.get(2016, 4, 23), null, "Ronald Peuchère", null, "CH7400243243G15379860", coordonneesFinancieres.iterator().next());
	}

	/**
	 * Ce test vérifie que l'ajout de coordonnées financières invalides sur un contribuable vide met quand-même à jour les coordonnées financières.
	 */
	@Test
	public void testDetectAndUpdateIbanInvalideSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH7", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car il n'y a pas d'ancien iban"));

		// on vérifie que les coordonnées financières ont bien été ajoutées
		final Set<CoordonneesFinancieres> coordonneesFinancieres = ctb.getCoordonneesFinancieres();
		assertNotNull(coordonneesFinancieres);
		assertEquals(1, coordonneesFinancieres.size());
		assertCoordFin(RegDate.get(2016, 4, 23), null, "Ronald Peuchère", null, "CH7", coordonneesFinancieres.iterator().next());
	}

	/**
	 * Ce test vérifie que l'ajout d'un iban incomplet (= 'CH' : considéré comme nul) sur un contribuable vide ne met pas à jour les coordonnées financières.
	 */
	@Test
	public void testDetectAndUpdateIbanIncompletSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car il n'y a pas d'ancien iban"));

		// on vérifie que les coordonnées financières n'ont pas été ajoutées
		assertEmpty(ctb.getCoordonneesFinancieres());
	}

	/**
	 * Ce test vérifie que l'ajout de coordonnées financières valides sur un contribuable avec un iban valide fonctionne bien.
	 */
	@Test
	public void testDetectAndUpdateIbanValideSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, null, "CH2409000000180252777", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est valide"));

		// on vérifie que les coordonnées financières ont bien été ajoutées et historisées
		final List<CoordonneesFinancieres> coordonneesFinancieres = new ArrayList<>(ctb.getCoordonneesFinancieres());
		assertNotNull(coordonneesFinancieres);
		coordonneesFinancieres.sort(DateRangeComparator::compareRanges);
		assertEquals(2, coordonneesFinancieres.size());
		assertCoordFin(null, RegDate.get(2016, 4, 22), "RONALD PEUCHERE", null, "CH7400243243G15379860", coordonneesFinancieres.get(0));
		assertCoordFin(RegDate.get(2016, 4, 23), null, "RONALD PEUCHERE", null, "CH2409000000180252777", coordonneesFinancieres.get(1));
	}

	/**
	 * Ce test vérifie que l'ajout d'un iban invalide sur un contribuable avec un iban valide ne met pas à jour les coordonnées financières et informe l'appelant que l'ancien iban a été gardé.
	 */
	@Test
	public void testDetectAndUpdateIbanInvalideSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		final MutableBoolean callbackAppele = new MutableBoolean(false);

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH7", RegDate.get(2016, 4, 23), (currentIban, newIban) -> callbackAppele.setTrue());

		// on devrait avoir reçu un warning parce que le nouvel IBAN n'est pas valide
		assertTrue(callbackAppele.booleanValue());

		// on vérifie que les coordonnées financières n'ont pas été changées
		final Set<CoordonneesFinancieres> coordonneesFinancieres = ctb.getCoordonneesFinancieres();
		assertNotNull(coordonneesFinancieres);
		assertEquals(1, coordonneesFinancieres.size());
		assertCoordFin(null, null, "RONALD PEUCHERE", null, "CH7400243243G15379860", coordonneesFinancieres.iterator().next());
	}

	/**
	 * Ce test vérifie que l'ajout d'un iban incomplet (= 'CH' : considéré comme nul) sur un contribuable avec un iban valide ne met pas à jour les coordonnées financières.
	 */
	@Test
	public void testDetectAndUpdateIbanIncompletSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		final MutableBoolean callbackAppele = new MutableBoolean(false);

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH", RegDate.get(2016, 4, 23), (currentIban, newIban) -> callbackAppele.setTrue());

		// on devrait avoir reçu un warning parce que le nouvel IBAN n'est pas valide
		assertTrue(callbackAppele.booleanValue());

		// on vérifie que les coordonnées financières n'ont pas été ajoutées
		final Set<CoordonneesFinancieres> coordonneesFinancieres = ctb.getCoordonneesFinancieres();
		assertNotNull(coordonneesFinancieres);
		assertEquals(1, coordonneesFinancieres.size());
		assertCoordFin(null, null, "RONALD PEUCHERE", null, "CH7400243243G15379860", coordonneesFinancieres.iterator().next());
	}

	/**
	 * Ce test vérifie que l'ajout d'un titulaire sans iban sur un contribuable vide ne fait rien.
	 */
	@Test
	public void testDetectAndUpdateTitulaireSansIbanSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", null, RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est nul"));

		// on vérifie que rien n'a changé
		assertEmpty(ctb.getCoordonneesFinancieres());
	}


	/**
	 * Ce test vérifie que l'ajout d'un titulaire sans iban sur un contribuable avec un iban valide met bien à jour le titulaire en gardant l'iban existant.
	 */
	@Test
	public void testDetectAndUpdateTitulaireSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", null, RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est valide"));

		// on vérifie que les coordonnées financières ont bien été ajoutées et historisées
		final List<CoordonneesFinancieres> coordonneesFinancieres = new ArrayList<>(ctb.getCoordonneesFinancieres());
		assertNotNull(coordonneesFinancieres);
		coordonneesFinancieres.sort(DateRangeComparator::compareRanges);
		assertEquals(2, coordonneesFinancieres.size());
		assertCoordFin(null, RegDate.get(2016, 4, 22), "RONALD PEUCHERE", null, "CH7400243243G15379860", coordonneesFinancieres.get(0));
		assertCoordFin(RegDate.get(2016, 4, 23), null, "Ronald Peuchère", null, "CH7400243243G15379860", coordonneesFinancieres.get(1));
	}

	/**
	 * Ce test vérifie que l'ajout d'un titulaire sans iban sur un contribuable avec un iban invalide ne fait rien.
	 */
	@Test
	public void testDetectAndUpdateTitulaireSurCtbAvecIbanInvalide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7", null));

		// on ajoute les coordonnées financières
		service.detectAndUpdateCoordonneesFinancieres(ctb, "Ronald Peuchère", null, RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car le nouvel  iban est vide"));

		// on vérifie que les coordonnées financières n'ont pas été changées
		final Set<CoordonneesFinancieres> coordonneesFinancieres = ctb.getCoordonneesFinancieres();
		assertNotNull(coordonneesFinancieres);
		assertEquals(1, coordonneesFinancieres.size());
		assertCoordFin(null, null, "RONALD PEUCHERE", null, "CH7", coordonneesFinancieres.iterator().next());
	}

	private static void assertCoordFin(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String titulaire, @Nullable String bicSwift, @Nullable String iban, CoordonneesFinancieres coord) {
		assertNotNull(coord);
		assertEquals(dateDebut, coord.getDateDebut());
		assertEquals(dateFin, coord.getDateFin());
		assertEquals(titulaire, coord.getTitulaire());
		assertCompteBancaire(bicSwift, iban, coord);
	}

	private static void assertCompteBancaire(@Nullable String bicSwift, @Nullable String iban, CoordonneesFinancieres coord) {
		final CompteBancaire compteBancaire = coord.getCompteBancaire();
		assertEquals(bicSwift, compteBancaire.getBicSwift());
		assertEquals(iban, compteBancaire.getIban());
	}
}