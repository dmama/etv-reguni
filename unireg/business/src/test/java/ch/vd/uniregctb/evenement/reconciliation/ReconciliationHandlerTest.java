package ch.vd.uniregctb.evenement.reconciliation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ReconciliationHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(ReconciliationHandlerTest.class);

	/**
	 * Le numero d'individu celibataire.
	 */
	private static final Long NO_INDIVIDU_SEPARE_SEUL = 12345L;

	/**
	 * Numeros des individus marié.
	 */
	private static final Long NO_INDIVIDU_SEPARE_MARIE = 54321L;
	private static final Long NO_INDIVIDU_SEPARE_MARIE_CONJOINT = 23456L;

	/**
	 * La date de séparation
	 */
	private static final RegDate DATE_SEPARATION = RegDate.get(2007, 3, 24);

	/**
	 * La date de reconciliation
	 */
	private static final RegDate DATE_RECONCILIATION = RegDate.get(2008, 12, 1);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ReconciliationHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testReconciliationMarieSeul() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de réconciliation d'un individu marié seul.");
		MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(NO_INDIVIDU_SEPARE_SEUL, 2008);
		final Reconciliation reconciliation = createReconciliation(individu, null, DATE_RECONCILIATION);

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		doInNewTransaction(new TransactionCallback(){

			public Object doInTransaction(TransactionStatus status) {
				evenementCivilHandler.checkCompleteness(reconciliation, erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de la réconciliation.", erreurs);

				evenementCivilHandler.validate(reconciliation, erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du validate de la réconciliation.", erreurs);

				evenementCivilHandler.handle(reconciliation, warnings);

				return null;
			}
		});

		PersonnePhysique habitantReconcilie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SEPARE_SEUL);
		assertNotNull("Le tiers correspondant au réconcilié n'a pas été trouvé", habitantReconcilie);
		assertNull("Le for principal du réconcilié n'a pas été fermé", habitantReconcilie.getForFiscalPrincipalAt(null));

		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(habitantReconcilie, DATE_RECONCILIATION);
		assertNotNull("L'ensemble tier couple n'a pas été trouvé", ensembleTiersCouple);

		MenageCommun menage = ensembleTiersCouple.getMenage();
		assertNotNull("Le tiers correspondant au ménage commun n'a pas été trouvé", menage);
		assertNotNull("Le for principal du tiers ménage n'a pas été ouvert", menage.getForFiscalPrincipalAt(null));

		ForFiscalPrincipal ffp = menage.getForFiscalPrincipalAt(DATE_RECONCILIATION);
		assertNull("Le for principal du ménage est fermmé", ffp.getDateFin());
		RapportEntreTiers[] rapports = habitantReconcilie.getRapportsSujet().toArray(new RapportEntreTiers[0]);
		Arrays.sort(rapports, new Comparator<RapportEntreTiers>() {
			public int compare(RapportEntreTiers r1, RapportEntreTiers r2) {
				return r1.getDateDebut().compareTo(r2.getDateDebut());
			}
		});
		RapportEntreTiers rapportSepares = rapports[0];
		assertEquals("le rapport entre tiers séparés n'a pas été clos", DATE_SEPARATION.getOneDayBefore(), rapportSepares.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture for fiscal principal sur le tiers
		 */
		assertEquals(2, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(habitantReconcilie).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menage).size());
	}

	@Test
	public void testReconciliationMariesADeux() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de réconciliation d'un couple.");
		MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(NO_INDIVIDU_SEPARE_MARIE, 2008);
		MockIndividu conjoint = (MockIndividu) serviceCivil.getIndividu(NO_INDIVIDU_SEPARE_MARIE_CONJOINT, 2008);
		final Reconciliation reconciliation = createReconciliation(individu, conjoint, DATE_RECONCILIATION);

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		doInNewTransaction(new TransactionCallback(){

			public Object doInTransaction(TransactionStatus status) {
				evenementCivilHandler.checkCompleteness(reconciliation, erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de la réconciliation.", erreurs);

				evenementCivilHandler.validate(reconciliation, erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du validate de la réconciliation.", erreurs);

				evenementCivilHandler.handle(reconciliation, warnings);

				return null;
			}
		});

		PersonnePhysique habitantReconcilie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SEPARE_MARIE);
		assertNotNull("Le tiers correspondant au réconcilié n'a pas été trouvé", habitantReconcilie);
		assertNull("Le for principal du réconcilié n'a pas été fermé", habitantReconcilie.getForFiscalPrincipalAt(null));

		PersonnePhysique conjointReconcilie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SEPARE_MARIE_CONJOINT);
		assertNotNull("Le tiers correspondant au conjoint du réconcilié n'a pas été trouvé", conjointReconcilie);
		assertNull("Le for principal conjoint n'a pas été fermé", conjointReconcilie.getForFiscalPrincipalAt(null));

		EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(habitantReconcilie, DATE_RECONCILIATION);
		assertNotNull("L'ensemble tier couple n'a pas été trouvé", ensembleTiersCouple);

		MenageCommun menage = ensembleTiersCouple.getMenage();
		assertNotNull("Le tiers correspondant au ménage commun n'a pas été trouvé", menage);
		assertNotNull("Le for principal du tiers ménage n'a pas été ouvert", menage.getForFiscalPrincipalAt(null));

		ForFiscalPrincipal ffp = menage.getForFiscalPrincipalAt(DATE_RECONCILIATION);
		assertNull("Le for principal du ménage est fermmé", ffp.getDateFin());
		RapportEntreTiers[] rapports = habitantReconcilie.getRapportsSujet().toArray(new RapportEntreTiers[0]);
		Arrays.sort(rapports, new Comparator<RapportEntreTiers>() {
			public int compare(RapportEntreTiers r1, RapportEntreTiers r2) {
				return r1.getDateDebut().compareTo(r2.getDateDebut());
			}
		});
		RapportEntreTiers rapportSepares = rapports[0];
		assertEquals("le rapport entre tiers séparés n'a pas été clos", DATE_SEPARATION.getOneDayBefore(), rapportSepares.getDateFin());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur le ménage commun
		 *  - ouverture fors fiscaux sur les tiers
		 */
		assertEquals(3, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(habitantReconcilie).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(conjointReconcilie).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menage).size());
	}

	@Test
	public void testReconciliationDateFuture() throws Exception {

		RegDate DATE_RECONCILIATION_FUTURE = RegDate.get(2080, 1, 1);
		LOGGER.debug("Test de traitement d'un événement de réconciliation d'un couple.");
		MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(NO_INDIVIDU_SEPARE_MARIE, 2008);
		MockIndividu conjoint = (MockIndividu) serviceCivil.getIndividu(NO_INDIVIDU_SEPARE_MARIE_CONJOINT, 2008);
		final Reconciliation reconciliation = createReconciliation(individu, conjoint, DATE_RECONCILIATION_FUTURE);

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		doInNewTransaction(new TransactionCallback(){

			public Object doInTransaction(TransactionStatus status) {
				evenementCivilHandler.checkCompleteness(reconciliation, erreurs, warnings);
				assertEmpty("Une erreur est survenue lors du checkCompleteness de la réconciliation.", erreurs);

				evenementCivilHandler.validate(reconciliation, erreurs, warnings);

				return null;
			}
		});

		assertEquals("Il devrait y avoir exactement une erreur", 1, erreurs.size());

		final EvenementCivilErreur erreur = erreurs.iterator().next();
		assertEquals("L'erreur n'est pas la bonne", "La date de l'événement est dans le futur", erreur.getMessage());
	}

	private Reconciliation createReconciliation(Individu individu, Individu conjoint, RegDate date) {
		/*
		 * Simulation de séparation
		 */
		individu.getEtatsCivils().add(createEtatCivilSeparation(individu, conjoint, DATE_SEPARATION));
		individu.getEtatsCivils().add(createEtatCivilReconciliation(individu, conjoint, DATE_RECONCILIATION));
		if (conjoint != null) {
			conjoint.getEtatsCivils().add(createEtatCivilSeparation(conjoint, individu, DATE_SEPARATION));
			conjoint.getEtatsCivils().add(createEtatCivilReconciliation(conjoint, individu, DATE_RECONCILIATION));
		}


		MockReconciliation reconciliation = new MockReconciliation();
		reconciliation.setType(TypeEvenementCivil.RECONCILIATION);
		reconciliation.setIndividu(individu);
		reconciliation.setConjoint(conjoint);
		reconciliation.setNumeroOfsCommuneAnnonce(5586);
		reconciliation.setDate(date);
		return reconciliation;
	}

	private EtatCivil createEtatCivilSeparation(Individu individu, Individu conjoint, RegDate dateSeparation) {
		MockEtatCivil separation = new MockEtatCivil();
		separation.setDateDebutValidite(dateSeparation);
		separation.setNoSequence(individu.getEtatsCivils().size());
		separation.setTypeEtatCivil(EnumTypeEtatCivil.SEPARE);
		if(conjoint!=null){
		  separation.setNumeroConjoint(conjoint.getNoTechnique());	
		}
		return separation;
	}

	private EtatCivil createEtatCivilReconciliation(Individu individu, Individu conjoint, RegDate dateReconciliation) {
		MockEtatCivil marie = new MockEtatCivil();
		marie.setDateDebutValidite(dateReconciliation);
		marie.setNoSequence(individu.getEtatsCivils().size());
		marie.setTypeEtatCivil(EnumTypeEtatCivil.MARIE);
		if(conjoint!=null){
		  marie.setNumeroConjoint(conjoint.getNoTechnique());
		}
		return marie;
	}
}
