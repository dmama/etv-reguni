package ch.vd.uniregctb.evenement.engine;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.AuditLine;
import ch.vd.uniregctb.audit.AuditLineDAO;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaireDAO;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Tests unitaire du service de regroupement des événements unitaires.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class EvenementCivilRegrouperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilRegrouperTest.class);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "EvenementCivilRegrouperTest.xml";

	/**
	 *
	 */
	private static final Long NUMERO_INDIVIDU_SEUL = 12345L;

	/**
	 *
	 */
	private static final Long NUMERO_INDIVIDU_MARIE = 54321L;

	/**
	 *
	 */
	private static final Long NUMERO_INDIVIDU_CONJOINT = 23456L;

	/**
	 * L'implémentation du service de regroupement des événements civils unitaires.
	 */
	EvenementCivilRegrouper evenementCivilRegrouper = null;

	/**
	 * La DAO pour les evenements
	 */
	EvenementCivilRegroupeDAO evenementCivilRegroupeDAO = null;

	/**
	 * La DAO pour les evenements
	 */
	EvenementCivilUnitaireDAO evenementCivilUnitaireDAO = null;

	AuditLineDAO auditLineDAO = null;

	/**
	 * Crée la connexion à la base de données
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil());

		evenementCivilRegrouper = getBean(EvenementCivilRegrouper.class, "evenementCivilRegrouper");
		evenementCivilRegroupeDAO = getBean(EvenementCivilRegroupeDAO.class, "evenementCivilRegroupeDAO");
		evenementCivilUnitaireDAO = getBean(EvenementCivilUnitaireDAO.class, "evenementCivilUnitaireDAO");
		auditLineDAO = getBean(AuditLineDAO.class, "auditLineDAO");
	}

	/**
	 * Teste la méthode de regroupement des nouveaux événments civils unitaires.
	 *
	 * @throws Exception
	 */
	@Test
	public void testRegroupeNouveauxEvenements() throws Exception {
		LOGGER.debug("Test de regroupement des événements unitaires.");

		/* Charge avec le fichier DbUnit spécifié plus haut */
		loadDatabase(DB_UNIT_DATA_FILE);

		/* Lancement du regroupement des événements */
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				evenementCivilRegrouper.regroupeTousEvenementsNonTraites();
				return null;
			}
		});

		/* Les événements civils unitaires doivent tous avoir l'état TRAITE */
		List<EvenementCivilUnitaire> evenementsUnitaires = evenementCivilUnitaireDAO.getAll();
		assertEquals("Le nombre d'événements unitaires est incorrect", 3, evenementsUnitaires.size());
		for (EvenementCivilUnitaire evenementUnitaire : evenementsUnitaires) {
			EtatEvenementCivil etat = evenementUnitaire.getEtat();
			assertEquals("L'état de l'événement "+ evenementUnitaire.getId() +" incorrect", EtatEvenementCivil.TRAITE, etat);
		}

		/* Le nombre d'événements regroupés doit être 2 */
		List<EvenementCivilRegroupe> evenements = evenementCivilRegroupeDAO.getAll();
		assertEquals("Le nombre d'événements regroupés est incorrect", 2, evenements.size());

		/* on teste les données des événements regroupés */
		for (EvenementCivilRegroupe evenement : evenements) {

			// 1er cas : le couple dans l'ordre
			if ( (evenement.getNumeroIndividuPrincipal().equals(NUMERO_INDIVIDU_MARIE) ) ) {
				assertEquals("le numero du conjoint n'a pas été récupéré", NUMERO_INDIVIDU_CONJOINT, evenement.getNumeroIndividuConjoint());
				assertNotNull("l'habitant pricipal n'a pas été récupéré", evenement.getHabitantPrincipal());
				assertNotNull("l'habitant conjoint n'a pas été récupéré", evenement.getHabitantConjoint());
			}

			// 2eme cas : le couple l'ordre inverse
			else if ( (evenement.getNumeroIndividuPrincipal().equals(NUMERO_INDIVIDU_CONJOINT) ) ) {
				assertEquals("le numero du conjoint n'a pas été récupéré", NUMERO_INDIVIDU_MARIE, evenement.getNumeroIndividuConjoint());
				Assert.notNull( evenement.getHabitantPrincipal(), "l'habitant pricipal n'a pas été récupéré");
				Assert.notNull( evenement.getHabitantConjoint(), "l'habitant conjoint n'a pas été récupéré");
			}

			// 3ème cas : le celibataire
			else if ( (evenement.getNumeroIndividuPrincipal().equals(NUMERO_INDIVIDU_SEUL) ) ) {
				Assert.notNull( evenement.getHabitantPrincipal(), "l'habitant pricipal n'a pas été récupéré");
				Assert.isNull( evenement.getHabitantConjoint(), "Un habitant conjoint a pas été récupéré par erreur");
				Assert.isNull( evenement.getNumeroIndividuConjoint(), "Un numero d'individu conjoint a pas été récupéré par erreur");
			}

			else {
				Assert.isTrue(false, "Evenement civil regroupé invalide");
			}
		}

		// Valide que l'audit est créé
		List<AuditLine> list = auditLineDAO.findLastCountFromID(0, AuditLineDAO.DEFAULT_BATCH_SIZE);
		assertTrue(list.size() > 0);
	}

	@Test
	public void testDateInvalide() throws Exception {

		final Long id = 1234L;
		final Long numeroIndividu = 45678L;
		final Integer numeroOFS = 5586;
		final RegDate date = null;
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				EvenementCivilUnitaire evUnitaire = new EvenementCivilUnitaire();
				evUnitaire.setId(id);
				evUnitaire.setType(TypeEvenementCivil.NAISSANCE);
				evUnitaire.setDateEvenement(date);
				evUnitaire.setEtat(EtatEvenementCivil.A_TRAITER);
				evUnitaire.setNumeroIndividu(numeroIndividu);
				evUnitaire.setNumeroOfsCommuneAnnonce(numeroOFS); // Inconnu
				evenementCivilUnitaireDAO.save(evUnitaire);
				return null;
			}
		});

		{
			// Traite l'evt ci-dessus (Il doit etre en erreur)
			StringBuffer errorMsg = new StringBuffer();
			evenementCivilRegrouper.regroupeUnEvenementById(id, errorMsg);

			// Check que l'evt unitaire est en erreur
			EvenementCivilUnitaire evUnitaire = evenementCivilUnitaireDAO.get(id);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evUnitaire.getEtat());

			assertContains("La date ", errorMsg.toString());
			assertContains(" est invalide", errorMsg.toString());
		}
	}

}
