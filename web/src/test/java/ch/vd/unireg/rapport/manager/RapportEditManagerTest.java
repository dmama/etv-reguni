package ch.vd.unireg.rapport.manager;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationException;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.evenement.fiscal.CollectingEvenementFiscalSender;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalCommunaute;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rapport.TypeRapportEntreTiersWeb;
import ch.vd.unireg.rapport.view.RapportView;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.tiers.AssujettissementParSubstitution;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RepresentationConventionnelle;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class RapportEditManagerTest extends WebTest {

	private RapportEditManager manager;
	private CollectingEvenementFiscalSender evenementFiscalSender;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		manager = getBean(RapportEditManager.class, "rapportEditManager");
		evenementFiscalSender = getBean(CollectingEvenementFiscalSender.class, "evenementFiscalSender");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");

		evenementFiscalSender.reset();
	}

	/**
	 * [UNIREG-1341/UNIREG-2655] Il ne doit pas être possible d'ajouter une représentation conventionnelle avec extension de l'exécution forcée sur un contribuable vaudois
	 */
	@Test
	public void testAjouterRepresentationConventionnelleSurCtbVaudois() throws Exception {

		final long noIndRepresentant = 1L;
		final long noIndRepresente = 2L;

		final long noTiersRepresentant = 10000001L;
		final long noTiersRepresente = 10000002L;

		// Crée deux tiers habitant dans le canton

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndRepresentant, date(1957, 5, 23), "Lavanchy", "Simon", true);
				addIndividu(noIndRepresente, date(1964, 5, 23), "Poulet", "Jean-Patrice", true);
			}
		});

		doInNewTransaction(status -> {
			addHabitant(noTiersRepresentant, noIndRepresentant);
			PersonnePhysique represente = addHabitant(noTiersRepresente, noIndRepresente);
			addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Bex);
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
			rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
			rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
			rapport.setDateDebut(date(2010, 1, 1));

			// vérifie qu'il n'est PAS possible de sauver une représentation AVEC extension de l'exécution forcée
			try {
				rapport.setExtensionExecutionForcee(true);
				manager.add(rapport);
				fail("Il ne devrait pas être possible de sauver un rapport de représentation avec extension de l'exécution forcée sur un habitant");
			}
			catch (ValidationException e) {
				assertEquals("PersonnePhysique #10000002 - 1 erreur(s) - 0 avertissement(s):\n" +
						             " [E] L'extension de l'exécution forcée est uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse\n", e.getMessage());
			}

			// vérifie qu'il EST possible de sauver une représentation SANS extension de l'exécution forcée
			rapport.setExtensionExecutionForcee(false);
			manager.add(rapport);
			return null;
		});

		assertUneRepresentationConventionnelle(false, noTiersRepresente);
	}

	/**
	 * [UNIREG-1341/UNIREG-2655] Il ne doit pas être possible d'ajouter une représentation conventionnelle avec extension de l'exécution forcée sur un contribuable hors-canton
	 */
	@Test
	public void testAjouterRepresentationConventionnelleSurCtbHorsCanton() throws Exception {

		final long noIndRepresentant = 1L;
		final long noIndRepresente = 2L;

		final long noTiersRepresentant = 10000001L;
		final long noTiersRepresente = 10000002L;

		// Crée deux tiers habitant dans le canton

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndRepresentant, date(1957, 5, 23), "Lavanchy", "Simon", true);
				addIndividu(noIndRepresente, date(1964, 5, 23), "Poulet", "Jean-Patrice", true);
			}
		});

		doInNewTransaction(status -> {
			addHabitant(noTiersRepresentant, noIndRepresentant);
			PersonnePhysique represente = addHabitant(noTiersRepresente, noIndRepresente);
			addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Zurich);
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
			rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
			rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
			rapport.setDateDebut(date(2010, 1, 1));

			// vérifie qu'il n'est PAS possible de sauver une représentation AVEC extension de l'exécution forcée
			try {
				rapport.setExtensionExecutionForcee(true);
				manager.add(rapport);
				fail("Il ne devrait pas être possible de sauver un rapport de représentation avec extension de l'exécution forcée sur un habitant");
			}
			catch (ValidationException e) {
				assertEquals("PersonnePhysique #10000002 - 1 erreur(s) - 0 avertissement(s):\n" +
						             " [E] L'extension de l'exécution forcée est uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse\n", e.getMessage());
			}

			// vérifie qu'il EST possible de sauver une représentation SANS extension de l'exécution forcée
			rapport.setExtensionExecutionForcee(false);
			manager.add(rapport);
			return null;
		});

		assertUneRepresentationConventionnelle(false, noTiersRepresente);
	}

	/**
	 * [UNIREG-1341/UNIREG-2655] Il doit être possible d'ajouter une représentation conventionnelle avec extension de l'exécution forcée sur un contribuable hors-Suisse
	 */
	@Test
	public void testAjouterRepresentationConventionnelleSurCtbHorsSuisse() throws Exception {

		final long noIndRepresentant = 1L;

		final long noTiersRepresentant = 10000001L;
		final long noTiersRepresente = 10000002L;

		// Crée un tiers habitant dans le canton et un autre non-habitant

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndRepresentant, date(1957, 5, 23), "Lavanchy", "Simon", true);
			}
		});

		doInNewTransaction(status -> {
			addHabitant(noTiersRepresentant, noIndRepresentant);
			PersonnePhysique represente = addNonHabitant(noTiersRepresente, "Jean-Patrice", "Poulet", date(1964, 5, 23), Sexe.MASCULIN);
			addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockPays.France);
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
			rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
			rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
			rapport.setDateDebut(date(2010, 1, 1));

			// vérifie qu'il EST possible de sauver une représentation AVEC extension de l'exécution forcée
			rapport.setExtensionExecutionForcee(true);
			manager.add(rapport);
			return null;
		});

		assertUneRepresentationConventionnelle(true, noTiersRepresente);
	}

	@Test
	public void testAjouterAssujettissementParSubstitutionSurCtbVaudois() throws Exception {

		final long noIndSubstituant = 1L;
		final long noIndSubstitue = 2L;

		final long noTiersSubstituant = 10000001L;
		final long noTiersSubstitue = 10000002L;

		// Crée deux tiers habitant dans le canton

		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndSubstituant, date(1957, 5, 23), "Lavanchy", "Simon", true);
				addIndividu(noIndSubstitue, date(1964, 5, 23), "Poulet", "Jean-Patrice", true);
			}
		});

		doInNewTransaction(status -> {
			addHabitant(noTiersSubstituant, noIndSubstituant);
			PersonnePhysique represente = addHabitant(noTiersSubstitue, noIndSubstitue);
			addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Bex);
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.ASSUJETTISSEMENT_PAR_SUBSTITUTION);
			rapport.setTiers(new TiersGeneralView(noTiersSubstituant));
			rapport.setTiersLie(new TiersGeneralView(noTiersSubstitue));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
			rapport.setDateDebut(date(2014, 11, 11));
			manager.add(rapport);
			return null;
		});

		assertUnAssujetissementParSubstitution(noTiersSubstitue);
	}

	/**
	 * [SIFISC-24999] Vérifie que le flag 'principal' est bien sauvé sur un premier rapport d'héritage.
	 */
	@Test
	public void testSaveRapportHeritage() throws Exception {

		class Ids {
			Long defunt;
			Long heritier;
		}
		final Ids ids = new Ids();

		// on créé deux personnes
		doInNewTransaction(status -> {
			final PersonnePhysique defunt = addNonHabitant("Jean", "Peuplu", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier = addNonHabitant("Jaime", "Rejoui", RegDate.get(1980, 1, 1), Sexe.MASCULIN);
			ids.defunt = defunt.getId();
			ids.heritier = heritier.getId();
			return null;
		});

		// on ajoute un lien d'héritage
		doInNewTransaction(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.HERITAGE);
			rapport.setTiers(new TiersGeneralView(ids.defunt));
			rapport.setTiersLie(new TiersGeneralView(ids.heritier));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);  // le tiers lié (héritier) est le sujet
			rapport.setDateDebut(date(2014, 11, 11));
			//rapport.setPrincipalCommunaute(true); ---> pas besoin de le mettre, la méthode l'ajoute automatiquement sur le premier héritage
			manager.add(rapport);
			return null;
		});

		// on vérifie que le rapport a bien été créé correctement
		doInNewTransaction(status -> {
			final PersonnePhysique defunt = hibernateTemplate.get(PersonnePhysique.class, ids.defunt);
			assertNotNull(defunt);

			final Set<RapportEntreTiers> rapportsObjets = defunt.getRapportsObjet();
			assertNotNull(rapportsObjets);
			assertEquals(1, rapportsObjets.size());

			final RapportEntreTiers rapport0 = rapportsObjets.iterator().next();
			assertNotNull(rapport0);
			assertTrue(rapport0 instanceof Heritage);

			final Heritage heritage0 = (Heritage) rapport0;
			assertEquals(ids.defunt, heritage0.getObjetId());
			assertEquals(ids.heritier, heritage0.getSujetId());
			assertTrue(heritage0.getPrincipalCommunaute());
			return null;
		});
	}

	/**
	 * Vérifie que le choix d'un principal valable à partir de la même date de début que le principal courant fonctionne bien.
	 */
	@Test
	public void testSetPrincipalMemeDateDebut() throws Exception {

		final RegDate dateHeritage = RegDate.get(2000, 1, 1);

		class Ids {
			Long defunt;
			Long heritier1;
			Long heritier2;
		}
		final Ids ids = new Ids();

		// on créé les personnes et les liens d'héritage
		doInNewTransaction(status -> {
			final PersonnePhysique defunt = addNonHabitant("Jean", "Peuplu", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Jaime", "Rejoui", RegDate.get(1980, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier2 = addNonHabitant("Annie", "Rejoui", RegDate.get(1980, 1, 1), Sexe.FEMININ);
			addHeritage(heritier1, defunt, dateHeritage, null, true);
			addHeritage(heritier2, defunt, dateHeritage, null, false);
			ids.defunt = defunt.getId();
			ids.heritier1 = heritier1.getId();
			ids.heritier2 = heritier2.getId();
			return null;
		});

		// on sélectionne le deuxième héritier comme principal à partir de la date d'héritage
		doInNewTransaction(status -> {
			manager.setPrincipal(ids.defunt, ids.heritier2, dateHeritage);
			return null;
		});

		// on vérifie que tout est ok
		doInNewTransaction(status -> {
			final Tiers defunt = tiersDAO.get(ids.defunt);
			final List<Heritage> heritages = defunt.getRapportsObjet().stream()
					.filter(Heritage.class::isInstance)
					.map(Heritage.class::cast)
					.sorted(Comparator.comparing(Heritage::getId))
					.collect(Collectors.toList());
			assertEquals(4, heritages.size());
			// les deux rapports préexistants sont annulés
			assertHeritage(dateHeritage, null, ids.defunt, ids.heritier1, true, true, heritages.get(0));
			assertHeritage(dateHeritage, null, ids.defunt, ids.heritier2, false, true, heritages.get(1));
			// l'héritier 2 est maintenant le principal à la place de l'héritier 1
			assertHeritage(dateHeritage, null, ids.defunt, ids.heritier1, false, false, heritages.get(2));
			assertHeritage(dateHeritage, null, ids.defunt, ids.heritier2, true, false, heritages.get(3));
			return null;
		});
	}

	/**
	 * Vérifie que le choix d'un principal valable à partir d'une date de début postérieur à la date d'héritage fonctionne bien.
	 */
	@Test
	public void testSetPrincipalDateDebutPosterieure() throws Exception {

		final RegDate dateHeritage = RegDate.get(2000, 1, 1);
		final RegDate dateChangement = RegDate.get(2004, 7, 1);

		class Ids {
			Long defunt;
			Long heritier1;
			Long heritier2;
		}
		final Ids ids = new Ids();

		// on créé les personnes et les liens d'héritage
		doInNewTransaction(status -> {
			final PersonnePhysique defunt = addNonHabitant("Jean", "Peuplu", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Jaime", "Rejoui", RegDate.get(1980, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier2 = addNonHabitant("Annie", "Rejoui", RegDate.get(1980, 1, 1), Sexe.FEMININ);
			addHeritage(heritier1, defunt, dateHeritage, null, true);
			addHeritage(heritier2, defunt, dateHeritage, null, false);
			ids.defunt = defunt.getId();
			ids.heritier1 = heritier1.getId();
			ids.heritier2 = heritier2.getId();
			return null;
		});

		// on sélectionne le deuxième héritier comme principal à une date postérieure au début de l'héritage
		doInNewTransaction(status -> {
			manager.setPrincipal(ids.defunt, ids.heritier2, dateChangement);
			return null;
		});

		// on vérifie que tout est ok
		doInNewTransaction(status -> {
			final Tiers defunt = tiersDAO.get(ids.defunt);
			final List<Heritage> heritages = defunt.getRapportsObjet().stream()
					.filter(Heritage.class::isInstance)
					.map(Heritage.class::cast)
					.sorted(Comparator.comparing(Heritage::getId))
					.collect(Collectors.toList());
			assertEquals(4, heritages.size());
			// les deux rapports préexistants sont fermés à la veille du changement
			assertHeritage(dateHeritage, dateChangement.getOneDayBefore(), ids.defunt, ids.heritier1, true, false, heritages.get(0));
			assertHeritage(dateHeritage, dateChangement.getOneDayBefore(), ids.defunt, ids.heritier2, false, false, heritages.get(1));
			// l'héritier 2 est maintenant le principal à la place de l'héritier 1
			assertHeritage(dateChangement, null, ids.defunt, ids.heritier1, false, false, heritages.get(2));
			assertHeritage(dateChangement, null, ids.defunt, ids.heritier2, true, false, heritages.get(3));
			return null;
		});
	}

	/**
	 * [SIFISC-24999] Vérifie qu'un événement fiscal sur la communauté RF est bien envoyé lorsqu'un nouveau rapport d'héritage est ajouté sur un membre existant d'une communauté.
	 */
	@Test
	public void testSaveRapportHeritageAvecCommunauteRF() throws Exception {

		final RegDate dateDebutHeritage = date(2014, 11, 11);

		class Ids {
			Long defunt;
			Long heritier;
			Long communaute;
		}
		final Ids ids = new Ids();

		// on créé deux personnes et une communauté RF à laquelle le défunt appartient.
		doInNewTransaction(status -> {
			// partie fiscale
			final PersonnePhysique defunt = addNonHabitant("Jean", "Peuplu", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier = addNonHabitant("Jaime", "Rejoui", RegDate.get(1980, 1, 1), Sexe.MASCULIN);

			// partie RF
			final BienFondsRF immeuble = addImmeubleRF("3893983");
			final PersonnePhysiqueRF ppRF1 = addPersonnePhysiqueRF("Jean", "Peuplu", RegDate.get(1920, 1, 1), "38383830ae3ff", 411451546L, null);
			final PersonnePhysiqueRF ppRF2 = addPersonnePhysiqueRF("Brigitte", "Widmer", date(1970, 7, 2), "434545", 411451L, null);

			final CommunauteRF communaute = addCommunauteRF("2892929", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			addDroitPropriete(ppRF1, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                                                                  date(2005, 3, 2), null,
			                                                                  null, null,
			                                                                  "Succession", null,
			                                                                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                                                                  "573853733gdbtq", "1");
			addDroitPropriete(ppRF2, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                                                                  date(2005, 3, 2), null,
			                                                                  null, null,
			                                                                  "Succession", null,
			                                                                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                                                                  "498238238d", "1");
			addRapprochementRF(defunt, ppRF1, date(2000, 1, 1), null, TypeRapprochementRF.AUTO);

			ids.defunt = defunt.getId();
			ids.heritier = heritier.getId();
			ids.communaute = communaute.getId();
			return null;
		});

		// on ajoute un lien d'héritage
		doInNewTransaction(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.HERITAGE);
			rapport.setTiers(new TiersGeneralView(ids.defunt));
			rapport.setTiersLie(new TiersGeneralView(ids.heritier));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);  // le tiers lié (héritier) est le sujet
			rapport.setDateDebut(dateDebutHeritage);
			manager.add(rapport);
			return null;
		});

		// on vérifie qu'un événement fiscal de changement sur la communauté a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		// Vérifie que l'événement est dans la base
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalCommunaute event0 = (EvenementFiscalCommunaute) events.get(0);
			assertEquals(EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.HERITAGE, event0.getType());
			assertEquals(dateDebutHeritage, event0.getDateValeur());
			assertEquals(ids.communaute, event0.getCommunaute().getId());
			return null;
		});
	}

	/**
	 * [SIFISC-24999] Vérifie qu'un événement fiscal sur la communauté RF est bien envoyé lorsqu'un nouveau rapport d'héritage est ajouté sur un membre existant d'une communauté.
	 */
	@Test
	public void testSetPrincipalAvecCommunauteRF() throws Exception {

		final RegDate dateHeritage = RegDate.get(2000, 1, 1);

		class Ids {
			Long defunt;
			Long heritier1;
			Long heritier2;
			Long communaute;
		}
		final Ids ids = new Ids();

		// on créé les personnes, les liens d'héritage et une communauté RF à laquelle le défunt appartient.
		doInNewTransaction(status -> {

			// partie fiscale
			final PersonnePhysique defunt = addNonHabitant("Jean", "Peuplu", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Jaime", "Rejoui", RegDate.get(1980, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier2 = addNonHabitant("Annie", "Rejoui", RegDate.get(1980, 1, 1), Sexe.FEMININ);
			addHeritage(heritier1, defunt, dateHeritage, null, true);
			addHeritage(heritier2, defunt, dateHeritage, null, false);

			// partie RF
			final BienFondsRF immeuble = addImmeubleRF("3893983");
			final PersonnePhysiqueRF ppRF1 = addPersonnePhysiqueRF("Jean", "Peuplu", RegDate.get(1920, 1, 1), "38383830ae3ff", 411451546L, null);
			final PersonnePhysiqueRF ppRF2 = addPersonnePhysiqueRF("Brigitte", "Widmer", date(1970, 7, 2), "434545", 411451L, null);

			final CommunauteRF communaute = addCommunauteRF("2892929", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			addDroitPropriete(ppRF1, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                  date(2005, 3, 2), null,
			                  null, null,
			                  "Succession", null,
			                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                  "573853733gdbtq", "1");
			addDroitPropriete(ppRF2, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                  date(2005, 3, 2), null,
			                  null, null,
			                  "Succession", null,
			                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                  "498238238d", "1");
			addRapprochementRF(defunt, ppRF1, date(2000, 1, 1), null, TypeRapprochementRF.AUTO);

			ids.defunt = defunt.getId();
			ids.heritier1 = heritier1.getId();
			ids.heritier2 = heritier2.getId();
			ids.communaute = communaute.getId();
			return null;
		});

		evenementFiscalSender.reset();

		// on sélectionne le deuxième héritier comme principal à partir de la date d'héritage
		doInNewTransaction(status -> {
			manager.setPrincipal(ids.defunt, ids.heritier2, dateHeritage);
			return null;
		});

		// on vérifie qu'un événement fiscal de changement sur la communauté a été envoyé
		assertEquals(1, evenementFiscalSender.getCount());

		// Vérifie que l'événement est dans la base
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalCommunaute event0 = (EvenementFiscalCommunaute) events.get(0);
			assertEquals(EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL, event0.getType());
			assertEquals(dateHeritage, event0.getDateValeur());
			assertEquals(ids.communaute, event0.getCommunaute().getId());
			return null;
		});
	}

	private static void assertHeritage(RegDate dateDebut, RegDate dateFin, Long defuntId, Long heritierId, boolean principal, boolean annule, Heritage heritage) {
		assertNotNull(heritage);
		assertEquals(dateDebut, heritage.getDateDebut());
		assertEquals(dateFin, heritage.getDateFin());
		assertEquals(annule, heritage.isAnnule());
		assertEquals(defuntId, heritage.getObjetId());
		assertEquals(heritierId, heritage.getSujetId());
		assertEquals(principal, heritage.getPrincipalCommunaute());
	}

	private void assertUneRepresentationConventionnelle(final boolean executionForcee, final long noTiersRepresente) throws Exception {
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique represente = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, noTiersRepresente);
			assertNotNull(represente);

			final Set<RapportEntreTiers> rapports = represente.getRapportsSujet();
			assertNotNull(rapports);
			assertEquals(1, rapports.size());

			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapports.iterator().next();
			assertEquals(executionForcee, repres.getExtensionExecutionForcee());
			return null;
		});
	}

	private void assertUnAssujetissementParSubstitution(final long noTiersSubstitue) throws Exception {
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique represente = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, noTiersSubstitue);
			assertNotNull(represente);

			final Set<RapportEntreTiers> rapports = represente.getRapportsSujet();
			assertNotNull(rapports);
			assertEquals(1, rapports.size());

			final RapportEntreTiers rapport = rapports.iterator().next();
			assertTrue(rapport instanceof AssujettissementParSubstitution);
			return null;
		});
	}
}
