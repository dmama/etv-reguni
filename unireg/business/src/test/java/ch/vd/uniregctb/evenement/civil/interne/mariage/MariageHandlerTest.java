package ch.vd.uniregctb.evenement.civil.interne.mariage;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class MariageHandlerTest extends AbstractEvenementHandlerTest {

	private static final Logger LOGGER = Logger.getLogger(MariageHandlerTest.class);

	/**
	 * Le numéro d'individu du marie seul.
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
	}

	@Test
	public void testMariageNonHabitantConnuAuCivilSepare() throws Exception {

		final long noIndMonsieur = 1234566L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noIndMonsieur, date(1956, 12, 5), "Talon", "Achille", true);
				addEtatCivil(monsieur, date(2005, 6, 1), TypeEtatCivil.SEPARE);
			}
		});

		final class Ids {
			long idMonsieur;
		}
		final Ids ids = new Ids();

		// mise en place ces contribuables
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique monsieur = addHabitant(noIndMonsieur);
				tiersService.changeHabitantenNH(monsieur);
				addForPrincipal(monsieur, date(2008, 1, 1), MotifFor.DEPART_HS, MockPays.France);

				ids.idMonsieur = monsieur.getNumero();
				return null;
			}
		});

		final class Lists {
			public final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
			public final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		}
		final Lists lists = new Lists();

		// mariage (devrait fonctionner même si monsieur est connu comme séparé - son divorce n'est pas encore connu du canton...)
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final Individu monsieur = serviceCivil.getIndividu(noIndMonsieur, 2009);
				final MariageAdapter mariage = createValidMariage(monsieur, null, date(2009, 2, 14));

				mariage.checkCompleteness(lists.erreurs, lists.warnings);
				mariage.validate(lists.erreurs, lists.warnings);
				mariage.handle(lists.warnings);
				return null;
			}
		});

		if (!lists.erreurs.isEmpty()) {
			for (EvenementCivilExterneErreur e : lists.erreurs) {
				LOGGER.error("Trouvé erreur : " + e);
			}
			Assert.fail("Il y a des erreurs...");
		}
		if (!lists.warnings.isEmpty()) {
			for (EvenementCivilExterneErreur e : lists.warnings) {
				LOGGER.error("Trouvé warning : " + e);
			}
			Assert.fail("Il y a des warnings...");
		}

		final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndMonsieur);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(monsieur, null);
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getMenage());
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertEquals(ids.idMonsieur, (long) couple.getPrincipal().getNumero());
		Assert.assertNull(couple.getConjoint());
	}

	/*
	 * Teste l'union seul d'un individu assujetti ordinaire
	 * @throws Exception
	 */
	@Test
	public void testHandleUnionSeulOrdinaire() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de mariage seul.");

		final class Lists {

			List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
			List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		}

		final Lists lists = new Lists();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu seul = serviceCivil.getIndividu(UNI_SEUL, 2007);
				MariageAdapter mariage = createValidMariage(seul, null, DATE_UNION_SEUL);

				mariage.checkCompleteness(lists.erreurs, lists.warnings);
				mariage.validate(lists.erreurs, lists.warnings);
				mariage.handle(lists.warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertTrue("Une erreur est survenue lors du traitement du mariage seul", lists.erreurs.isEmpty());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique pierre = tiersDAO.getHabitantByNumeroIndividu(UNI_SEUL);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", pierre);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Pierre ont disparus", pierre.getForsFiscaux());
		Assert.assertNull("Pierre ne devrait plus avoir de for principal actif après le mariage", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : pierre.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",  forCommun);
		Assert.assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date du mariage", DATE_UNION_SEUL, forCommun.getDateDebut());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur pierre
		 *  - ouverture for fiscal principal sur le ménage commun
		 */
		Assert.assertEquals(2, eventSender.count);
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(pierre).size());
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	/**
	 * Teste l'union seul de deux individus de sexes différents assujettis ordinaires
	 * @throws Exception
	 */
	@Test
	public void testHandleUnionHeteroOrdinaire() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement d'union hétéro.");

		final class Lists {

			List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
			List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		}

		final Lists lists = new Lists();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu individu = serviceCivil.getIndividu(UNI_HETERO, 2007);
				Individu conjoint = serviceCivil.getIndividu(UNI_HETERO_CONJOINT, 2007);
				MariageAdapter mariage = createValidMariage(individu, conjoint, DATE_UNION_HETERO);

				mariage.checkCompleteness(lists.erreurs, lists.warnings);
				mariage.validate(lists.erreurs, lists.warnings);
				mariage.handle(lists.warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertTrue("Une erreur est survenue lors du traitement de l'union hétéro", lists.erreurs.isEmpty());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique momo = tiersDAO.getHabitantByNumeroIndividu(UNI_HETERO);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", momo);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Momo ont disparus",  momo.getForsFiscaux());
		Assert.assertNull("Momo ne devrait plus avoir de for principal actif après le mariage", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé",  forFiscal.getDateFin());
		}

		/*
		 * Test de récupération du Conjoint
		 */
		final PersonnePhysique bea = tiersDAO.getHabitantByNumeroIndividu(UNI_HETERO_CONJOINT);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", bea);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Béa ont disparus", bea.getForsFiscaux());
		Assert.assertNull("Béa ne devrait plus avoir de for principal actif après le mariage",  bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : bea.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		final ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",  forCommun);
		Assert.assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date du mariage", DATE_UNION_HETERO, forCommun.getDateDebut());

		Assert.assertTrue("Il aurait dû y avoir au moins un for fiscal secondaire", menageCommun.getForsParType(false).secondaires.size() > 0);
		
		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur momo
		 *  - fermeture for fiscal principal sur bea
		 *  - ouverture for fiscal principal sur le ménage commun
		 */
		Assert.assertEquals(3, eventSender.count);
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(momo).size());
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(bea).size());
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	/**
	 * Teste l'union seul de deux individus de sexes identiques assujettis ordinaires
	 * @throws Exception
	 */
	@Test
	public void testHandleUnionHomoOrdinaire() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement d'union homo.");

		final class Lists {

			List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
			List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		}

		final Lists lists = new Lists();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu individu = serviceCivil.getIndividu(UNI_HOMO, 2007);
				Individu conjoint = serviceCivil.getIndividu(UNI_HOMO_CONJOINT, 2007);
				MariageAdapter mariage = createValidMariage(individu, conjoint, DATE_UNION_HOMO);

				mariage.checkCompleteness(lists.erreurs, lists.warnings);
				mariage.validate(lists.erreurs, lists.warnings);
				mariage.handle(lists.warnings);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertTrue("Une erreur est survenue lors du traitement de l'union homo", lists.erreurs.isEmpty());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique david = tiersDAO.getHabitantByNumeroIndividu(UNI_HOMO);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", david);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de David ont disparus", david.getForsFiscaux());
		Assert.assertNull("David ne devrait plus avoir de for principal actif après le mariage", david.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : david.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé",  forFiscal.getDateFin());
		}

		/*
		 * Test de récupération du Conjoint
		 */
		final PersonnePhysique julien = tiersDAO.getHabitantByNumeroIndividu(UNI_HOMO_CONJOINT);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numero individu (ou aucun)", julien);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Julien ont disparus", julien.getForsFiscaux());
		Assert.assertNull("Julien ne devrait plus avoir de for principal actif après le mariage", julien.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : julien.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : julien.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		final ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		Assert.assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date du mariage", DATE_UNION_HOMO, forCommun.getDateDebut());

		/*
		 * Evénements fiscaux devant être générés :
		 *  - fermeture for fiscal principal sur david
		 *  - fermeture for fiscal principal sur julien
		 *  - ouverture for fiscal principal sur le ménage commun
		 */
		Assert.assertEquals(3, eventSender.count);
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(david).size());
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(julien).size());
		Assert.assertEquals(1, getEvenementFiscalService().getEvenementsFiscaux(menageCommun).size());
	}

	private MariageAdapter createValidMariage(Individu individu, Individu conjoint, RegDate dateMariage) {

		final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
		final Long conjointPPId = (conjoint == null ? null : tiersDAO.getNumeroPPByNumeroIndividu(conjoint.getNoTechnique(), true));

		return new MariageAdapter(individu, principalPPId, conjoint, conjointPPId, dateMariage, 5586, context);
	}

}
