package ch.vd.uniregctb.evenement.mariage;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class MariageHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(MariageHandlerTest.class);

	/**
	 * Le numero d'individu du marie seul.
	 */
	private static final Long UNI_SEUL = 12345L;

	/**
	 * Les numeros d'individu des unis heteros
	 */
	private static final Long UNI_HETERO = 54321L;
	private static final Long UNI_HETERO_CONJOINT = 23456L;

	/**
	 * Les numeros d'individu des unis homos
	 */
	private static final Long UNI_HOMO = 56789L;
	private static final Long UNI_HOMO_CONJOINT = 45678L;

	/**
	 * Les dates d'union
	 */
	private static final RegDate DATE_UNION_SEUL = RegDate.get(1986, 4, 8);
	private static final RegDate DATE_UNION_HETERO = RegDate.get(1986, 4, 8);
	private static final RegDate DATE_UNION_HOMO = RegDate.get(1986, 4, 8);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "MariageHandlerTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/*
	 * Teste l'union seul d'un individu assujetti ordinaire
	 * @throws Exception
	 */
	@Test
	public void testHandleUnionSeulOrdinaire() throws Exception {

		LOGGER.debug("Test de traitement d'un événement de mariage seul.");

		final class Lists {

			List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
			List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		}

		final Lists lists = new Lists();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu seul = serviceCivil.getIndividu(UNI_SEUL, 2007);
				Mariage mariage = createValidMariage(seul, null, DATE_UNION_SEUL);

				evenementCivilHandler.checkCompleteness(mariage, lists.erreurs, lists.warnings);
				evenementCivilHandler.validate(mariage, lists.erreurs, lists.warnings);
				evenementCivilHandler.handle(mariage, lists.warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(lists.erreurs.isEmpty(), "Une erreur est survenue lors du traitement du mariage seul");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(UNI_SEUL);
		Assert.isTrue(pierre != null, "Plusieurs habitants trouvés avec le même numero individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( pierre.getForsFiscaux(), "Les for fiscaux de Pierre ont disparus" );
		Assert.isNull( pierre.getForFiscalPrincipalAt(null), "Pierre ne devrait plus avoir de for principal actif après le mariage" );
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : pierre.getRapportsSujet() ) {
			if ( rapport.getType().equals( TypeRapportEntreTiers.APPARTENANCE_MENAGE ) ) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) rapport.getObjet();
			}
		}
		Assert.isTrue( nbMenagesCommuns == 1, "Plusieurs ou aucun tiers MenageCommun ont été trouvés");

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.notNull( forCommun, "Aucun for fiscal principal trouvé sur le tiers MenageCommun" );
		Assert.isTrue( DATE_UNION_SEUL.equals(forCommun.getDateDebut()), "La date d'ouverture du nouveau for ne correspond pas a la date du mariage" );

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur pierre
		 *  - ouverture for fiscal principal sur le ménage commun
		 */
		assertEquals(2, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(pierre).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	/**
	 * Teste l'union seul de deux individus de sexes différents assujettis ordinaires
	 * @throws Exception
	 */
	@Test
	public void testHandleUnionHeteroOrdinaire() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'union hétéro.");

		final class Lists {

			List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
			List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		}

		final Lists lists = new Lists();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu individu = serviceCivil.getIndividu(UNI_HETERO, 2007);
				Individu conjoint = serviceCivil.getIndividu(UNI_HETERO_CONJOINT, 2007);
				Mariage mariage = createValidMariage(individu, conjoint, DATE_UNION_HETERO);

				evenementCivilHandler.checkCompleteness(mariage, lists.erreurs, lists.warnings);
				evenementCivilHandler.validate(mariage, lists.erreurs, lists.warnings);
				evenementCivilHandler.handle(mariage, lists.warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(lists.erreurs.isEmpty(), "Une erreur est survenue lors du traitement de l'union hétéro");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(UNI_HETERO);
		Assert.isTrue(momo != null, "Plusieurs habitants trouvés avec le même numero individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( momo.getForsFiscaux(), "Les for fiscaux de Momo ont disparus" );
		Assert.isNull( momo.getForFiscalPrincipalAt(null), "Momo ne devrait plus avoir de for principal actif après le mariage" );
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Test de récupération du Conjoint
		 */
		PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(UNI_HETERO_CONJOINT);
		Assert.isTrue(bea != null, "Plusieurs habitants trouvés avec le même numero individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( bea.getForsFiscaux(), "Les for fiscaux de Béa ont disparus" );
		Assert.isNull( bea.getForFiscalPrincipalAt(null), "Béa ne devrait plus avoir de for principal actif après le mariage" );
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : bea.getRapportsSujet() ) {
			if ( rapport.getType().equals( TypeRapportEntreTiers.APPARTENANCE_MENAGE ) ) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) rapport.getObjet();
			}
		}
		Assert.isTrue( nbMenagesCommuns == 1, "Plusieurs ou aucun tiers MenageCommun ont été trouvés");

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.notNull( forCommun, "Aucun for fiscal principal trouvé sur le tiers MenageCommun" );
		Assert.isTrue( DATE_UNION_HETERO.equals(forCommun.getDateDebut()), "La date d'ouverture du nouveau for ne correspond pas a la date du mariage" );

		Assert.isTrue(menageCommun.getForsParType(false).secondaires.size() > 0, "Il aurait dû y avoir au moins un for fiscal secondaire");
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur momo
		 *  - fermeture for fiscal principal sur bea
		 *  - ouverture for fiscal principal sur le ménage commun
		 */
		assertEquals(3, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(momo).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(bea).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	/**
	 * Teste l'union seul de deux individus de sexes identiques assujettis ordinaires
	 * @throws Exception
	 */
	@Test
	public void testHandleUnionHomoOrdinaire() throws Exception {

		LOGGER.debug("Test de traitement d'un événement d'union homo.");

		final class Lists {

			List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
			List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		}

		final Lists lists = new Lists();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu individu = serviceCivil.getIndividu(UNI_HOMO, 2007);
				Individu conjoint = serviceCivil.getIndividu(UNI_HOMO_CONJOINT, 2007);
				Mariage mariage = createValidMariage(individu, conjoint, DATE_UNION_HOMO);

				evenementCivilHandler.checkCompleteness(mariage, lists.erreurs, lists.warnings);
				evenementCivilHandler.validate(mariage, lists.erreurs, lists.warnings);
				evenementCivilHandler.handle(mariage, lists.warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.isTrue(lists.erreurs.isEmpty(), "Une erreur est survenue lors du traitement de l'union homo");

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique david = tiersDAO.getHabitantByNumeroIndividu(UNI_HOMO);
		Assert.isTrue(david != null, "Plusieurs habitants trouvés avec le même numero individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( david.getForsFiscaux(), "Les for fiscaux de David ont disparus" );
		Assert.isNull( david.getForFiscalPrincipalAt(null), "David ne devrait plus avoir de for principal actif après le mariage" );
		for (ForFiscal forFiscal : david.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Test de récupération du Conjoint
		 */
		PersonnePhysique julien = tiersDAO.getHabitantByNumeroIndividu(UNI_HOMO_CONJOINT);
		Assert.isTrue(julien != null, "Plusieurs habitants trouvés avec le même numero individu (ou aucun)");

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.notNull( julien.getForsFiscaux(), "Les for fiscaux de Julien ont disparus" );
		Assert.isNull( julien.getForFiscalPrincipalAt(null), "Julien ne devrait plus avoir de for principal actif après le mariage" );
		for (ForFiscal forFiscal : julien.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.notNull( forFiscal.getDateFin(), "Un for fiscal principal non fermé a été trouvé" );
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : julien.getRapportsSujet() ) {
			if ( rapport.getType().equals( TypeRapportEntreTiers.APPARTENANCE_MENAGE ) ) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) rapport.getObjet();
			}
		}
		Assert.isTrue( nbMenagesCommuns == 1, "Plusieurs ou aucun tiers MenageCommun ont été trouvés");

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.notNull( forCommun, "Aucun for fiscal principal trouvé sur le tiers MenageCommun" );
		Assert.isTrue( DATE_UNION_HOMO.equals(forCommun.getDateDebut()), "La date d'ouverture du nouveau for ne correspond pas a la date du mariage" );

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur david
		 *  - fermeture for fiscal principal sur julien
		 *  - ouverture for fiscal principal sur le ménage commun
		 */
		assertEquals(3, getMockEvenementFiscalFacade().count);
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(david).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(julien).size());
		assertEquals(1, getEvenementFiscalService().getEvenementFiscals(menageCommun).size());
	}

	private MockMariage createValidMariage(Individu individu, Individu conjoint, RegDate dateMariage) {

		MockMariage mariage = new MockMariage();
		mariage.setIndividu(individu);
		mariage.setNouveauConjoint(conjoint);

		mariage.setNumeroOfsCommuneAnnonce(5586);
		mariage.setDate( dateMariage );

		return mariage;
	}

}
