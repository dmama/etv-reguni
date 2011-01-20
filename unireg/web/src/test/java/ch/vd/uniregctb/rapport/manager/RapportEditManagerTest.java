package ch.vd.uniregctb.rapport.manager;

import java.util.Set;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
	@NotTransactional
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersRepresentant, noIndRepresentant);
				PersonnePhysique represente = addHabitant(noTiersRepresente, noIndRepresente);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Bex);
				return null;
			}
		});

		RapportView rapport = new RapportView();
		rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
		rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
		rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
		rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
		rapport.setDateDebut(date(2010, 1, 1));

		// vérifie qu'il n'est PAS possible de sauver une représentation AVEC extension de l'exécution forcée
		try {
			rapport.setExtensionExecutionForcee(true);
			manager.save(rapport);
			fail("Il ne devrait pas être possible de sauver un rapport de représentation avec extension de l'exécution forcée sur un habitant");
		}
		catch (ValidationException e) {
			assertEquals("PersonnePhysique #10000002 - 1 erreur(s) - 0 warning(s):\n" +
					" [E] L'extension de l'exécution forcée est uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse\n", e.getMessage());
		}

		// vérifie qu'il EST possible de sauver une représentation SANS extension de l'exécution forcée
		rapport.setExtensionExecutionForcee(false);
		manager.save(rapport);

		assertUneRepresentationConventionnelle(false, noTiersRepresente);
	}

	/**
	 * [UNIREG-1341/UNIREG-2655] Il ne doit pas être possible d'ajouter une représentation conventionnelle avec extension de l'exécution forcée sur un contribuable hors-canton
	 */
	@Test
	@NotTransactional
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersRepresentant, noIndRepresentant);
				PersonnePhysique represente = addHabitant(noTiersRepresente, noIndRepresente);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockCommune.Zurich);
				return null;
			}
		});

		RapportView rapport = new RapportView();
		rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
		rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
		rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
		rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
		rapport.setDateDebut(date(2010, 1, 1));

		// vérifie qu'il n'est PAS possible de sauver une représentation AVEC extension de l'exécution forcée
		try {
			rapport.setExtensionExecutionForcee(true);
			manager.save(rapport);
			fail("Il ne devrait pas être possible de sauver un rapport de représentation avec extension de l'exécution forcée sur un habitant");
		}
		catch (ValidationException e) {
			assertEquals("PersonnePhysique #10000002 - 1 erreur(s) - 0 warning(s):\n" +
					" [E] L'extension de l'exécution forcée est uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse\n", e.getMessage());
		}

		// vérifie qu'il EST possible de sauver une représentation SANS extension de l'exécution forcée
		rapport.setExtensionExecutionForcee(false);
		manager.save(rapport);

		assertUneRepresentationConventionnelle(false, noTiersRepresente);
	}

	/**
	 * [UNIREG-1341/UNIREG-2655] Il doit être possible d'ajouter une représentation conventionnelle avec extension de l'exécution forcée sur un contribuable hors-Suisse
	 */
	@Test
	@NotTransactional
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

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noTiersRepresentant, noIndRepresentant);
				PersonnePhysique represente = addNonHabitant(noTiersRepresente, "Jean-Patrice", "Poulet", date(1964, 5, 23), Sexe.MASCULIN);
				addForPrincipal(represente, date(1984, 5, 23), MotifFor.MAJORITE, MockPays.France);
				return null;
			}
		});

		RapportView rapport = new RapportView();
		rapport.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.REPRESENTATION);
		rapport.setTiers(new TiersGeneralView(noTiersRepresentant));
		rapport.setTiersLie(new TiersGeneralView(noTiersRepresente));
		rapport.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
		rapport.setDateDebut(date(2010, 1, 1));

		// vérifie qu'il EST possible de sauver une représentation AVEC extension de l'exécution forcée
		rapport.setExtensionExecutionForcee(true);
		manager.save(rapport);
		assertUneRepresentationConventionnelle(true, noTiersRepresente);
	}

	private void assertUneRepresentationConventionnelle(final boolean executionForcee, final long noTiersRepresente) throws Exception {
		doInNewTransactionAndSession(new TxCallback() {
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
}
