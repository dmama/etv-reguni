package ch.vd.unireg.coordfin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.tiers.CompteBancaire;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.PersonnePhysique;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

	/**
	 * Ce test vérifie que l'ajout de coordonnées financières valides sur un contribuable vide fonctionne bien.
	 */
	@Test
	public void testUpdateIbanValideSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH7400243243G15379860", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est valide"));

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
	public void testUpdateIbanInvalideSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH7", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car il n'y a pas d'ancien iban"));

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
	public void testUpdateIbanIncompletSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car il n'y a pas d'ancien iban"));

		// on vérifie que les coordonnées financières n'ont pas été ajoutées
		assertEmpty(ctb.getCoordonneesFinancieres());
	}

	/**
	 * Ce test vérifie que l'ajout de coordonnées financières valides sur un contribuable avec un iban valide fonctionne bien.
	 */
	@Test
	public void testUpdateIbanValideSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, null, "CH2409000000180252777", RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est valide"));

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
	public void testUpdateIbanInvalideSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		final MutableBoolean callbackAppele = new MutableBoolean(false);

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH7", RegDate.get(2016, 4, 23), (currentIban, newIban) -> callbackAppele.setTrue());

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
	public void testUpdateIbanIncompletSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		final MutableBoolean callbackAppele = new MutableBoolean(false);

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", "CH", RegDate.get(2016, 4, 23), (currentIban, newIban) -> callbackAppele.setTrue());

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
	public void testUpdateTitulaireSansIbanSurCtbVide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.setCoordonneesFinancieres(new HashSet<>());

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", null, RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est nul"));

		// on vérifie que rien n'a changé
		assertEmpty(ctb.getCoordonneesFinancieres());
	}


	/**
	 * Ce test vérifie que l'ajout d'un titulaire sans iban sur un contribuable avec un iban valide met bien à jour le titulaire en gardant l'iban existant.
	 */
	@Test
	public void testUpdateTitulaireSurCtbAvecIbanValide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7400243243G15379860", null));

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", null, RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car l'iban est valide"));

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
	public void testUpdateTitulaireSurCtbAvecIbanInvalide() {

		// on contribuable vide
		final Contribuable ctb = new PersonnePhysique();
		ctb.addCoordonneesFinancieres(new CoordonneesFinancieres("RONALD PEUCHERE", "CH7", null));

		// on ajoute les coordonnées financières
		service.updateCoordonneesFinancieres(ctb, "Ronald Peuchère", null, RegDate.get(2016, 4, 23), (currentIban, newIban) -> fail("on ne devrait pas passer par là car le nouvel  iban est vide"));

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