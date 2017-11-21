package ch.vd.uniregctb.rapport.manager;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class RapportEditManagerTest extends WebTest {

	private RapportEditManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		manager = getBean(RapportEditManager.class, "rapportEditManager");
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

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndRepresentant, date(1957, 5, 23), "Lavanchy", "Simon", true);
				addIndividu(noIndRepresente, date(1964, 5, 23), "Poulet", "Jean-Patrice", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersRepresentant, noIndRepresentant);
				PersonnePhysique represente = addHabitant(noTiersRepresente, noIndRepresente);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Bex);
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
			}
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

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndRepresentant, date(1957, 5, 23), "Lavanchy", "Simon", true);
				addIndividu(noIndRepresente, date(1964, 5, 23), "Poulet", "Jean-Patrice", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersRepresentant, noIndRepresentant);
				PersonnePhysique represente = addHabitant(noTiersRepresente, noIndRepresente);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Zurich);
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
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
			}
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

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndRepresentant, date(1957, 5, 23), "Lavanchy", "Simon", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersRepresentant, noIndRepresentant);
				PersonnePhysique represente = addNonHabitant(noTiersRepresente, "Jean-Patrice", "Poulet", date(1964, 5, 23), Sexe.MASCULIN);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockPays.France);
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final RapportView rapport = new RapportView();
				rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
				rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
				rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
				rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapport.setDateDebut(date(2010, 1, 1));

				// vérifie qu'il EST possible de sauver une représentation AVEC extension de l'exécution forcée
				rapport.setExtensionExecutionForcee(true);
				manager.add(rapport);
			}
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

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndSubstituant, date(1957, 5, 23), "Lavanchy", "Simon", true);
				addIndividu(noIndSubstitue, date(1964, 5, 23), "Poulet", "Jean-Patrice", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersSubstituant, noIndSubstituant);
				PersonnePhysique represente = addHabitant(noTiersSubstitue, noIndSubstitue);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Bex);
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final RapportView rapport = new RapportView();
				rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.ASSUJETTISSEMENT_PAR_SUBSTITUTION);
				rapport.setTiers(new TiersGeneralView(noTiersSubstituant));
				rapport.setTiersLie(new TiersGeneralView(noTiersSubstitue));
				rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
				rapport.setDateDebut(date(2014, 11, 11));
				manager.add(rapport);
			}
		});

		assertUnAssujetissementParSubstitution(noTiersSubstitue);
	}

	/**
	 * [SIFISC-24999] Vérifie que le flag 'principal' est bien sauvé sur un rapport d'héritage.
	 */
	@Test
	public void testSaveRapportHeritage() throws Exception {

		class Ids {
			Long decede;
			Long heritier;
		}
		final Ids ids = new Ids();

		// on créé deux personnes
		doInNewTransaction(status -> {
			final PersonnePhysique decede = addNonHabitant("Jean", "Peuplu", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier = addNonHabitant("Jaime", "Rejoui", RegDate.get(1980, 1, 1), Sexe.MASCULIN);
			ids.decede = decede.getId();
			ids.heritier = heritier.getId();
			return null;
		});

		// on ajoute un lien d'héritage
		doInNewTransaction(status -> {
			final RapportView rapport = new RapportView();
			rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.HERITAGE);
			rapport.setTiers(new TiersGeneralView(ids.decede));
			rapport.setTiersLie(new TiersGeneralView(ids.heritier));
			rapport.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET);  // le tiers lié est l'objet
			rapport.setDateDebut(date(2014, 11, 11));
			rapport.setPrincipalCommunaute(true);
			manager.add(rapport);
			return null;
		});

		// on vérifie que le rapport a bien été créé correctement
		doInNewTransaction(status -> {
			final PersonnePhysique decede = hibernateTemplate.get(PersonnePhysique.class, ids.decede);
			assertNotNull(decede);

			final Set<RapportEntreTiers> rapportsSujet = decede.getRapportsSujet();
			assertNotNull(rapportsSujet);
			assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers rapport0 = rapportsSujet.iterator().next();
			assertNotNull(rapport0);
			assertTrue(rapport0 instanceof Heritage);

			final Heritage heritage0 = (Heritage) rapport0;
			assertEquals(ids.decede, heritage0.getSujetId());
			assertEquals(ids.heritier, heritage0.getObjetId());
			assertTrue(heritage0.getPrincipalCommunaute());
			return null;
		});
	}

	private void assertUneRepresentationConventionnelle(final boolean executionForcee, final long noTiersRepresente) throws Exception {
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique represente = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, noTiersRepresente);
				assertNotNull(represente);

				final Set<RapportEntreTiers> rapports = represente.getRapportsSujet();
				assertNotNull(rapports);
				assertEquals(1, rapports.size());

				final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapports.iterator().next();
				assertEquals(executionForcee, repres.getExtensionExecutionForcee());
				return null;
			}
		});
	}

	private void assertUnAssujetissementParSubstitution(final long noTiersSubstitue) throws Exception {
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique represente = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, noTiersSubstitue);
				assertNotNull(represente);

				final Set<RapportEntreTiers> rapports = represente.getRapportsSujet();
				assertNotNull(rapports);
				assertEquals(1, rapports.size());

				final RapportEntreTiers rapport = rapports.iterator().next();
				assertTrue(rapport instanceof AssujettissementParSubstitution);
				return null;
			}
		});
	}
}
