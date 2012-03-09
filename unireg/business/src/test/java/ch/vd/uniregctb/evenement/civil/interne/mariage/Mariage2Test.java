package ch.vd.uniregctb.evenement.civil.interne.mariage;

import java.util.List;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class Mariage2Test extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(Mariage2Test.class);

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
	private static final String DB_UNIT_DATA_FILE = "Mariage2Test.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique monsieur = addHabitant(noIndMonsieur);
				tiersService.changeHabitantenNH(monsieur);
				addForPrincipal(monsieur, date(2008, 1, 1), MotifFor.DEPART_HS, MockPays.France);

				ids.idMonsieur = monsieur.getNumero();
				return null;
			}
		});

		final MessageCollector collector = buildMessageCollector();

		// mariage (devrait fonctionner même si monsieur est connu comme séparé - son divorce n'est pas encore connu du canton...)
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu monsieur = serviceCivil.getIndividu(noIndMonsieur, date(2009, 12, 31));
				final Mariage mariage = createValidMariage(monsieur, null, date(2009, 2, 14));

				mariage.validate(collector, collector);
				mariage.handle(collector);
				return null;
			}
		});

		if (collector.hasErreurs()) {
			for (EvenementCivilErreur e : collector.getErreurs()) {
				LOGGER.error("Trouvé erreur : " + e);
			}
			Assert.fail("Il y a des erreurs...");
		}
		if (collector.hasWarnings()) {
			for (EvenementCivilErreur e : collector.getWarnings()) {
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
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUnionSeulOrdinaire() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement de mariage seul.");

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu seul = serviceCivil.getIndividu(UNI_SEUL, date(2007, 12, 31));
				Mariage mariage = createValidMariage(seul, null, DATE_UNION_SEUL);

				mariage.validate(collector, collector);
				mariage.handle(collector);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertFalse("Une erreur est survenue lors du traitement du mariage seul", collector.hasErreurs());

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
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUnionHeteroOrdinaire() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement d'union hétéro.");

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu individu = serviceCivil.getIndividu(UNI_HETERO, date(2007, 12, 31));
				Individu conjoint = serviceCivil.getIndividu(UNI_HETERO_CONJOINT, date(2007, 12, 31));
				Mariage mariage = createValidMariage(individu, conjoint, DATE_UNION_HETERO);

				mariage.validate(collector, collector);
				mariage.handle(collector);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertFalse("Une erreur est survenue lors du traitement de l'union hétéro", collector.hasErreurs());

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

		Assert.assertTrue("Il aurait dû y avoir au moins un for fiscal secondaire", !menageCommun.getForsParType(false).secondaires.isEmpty());
		
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
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUnionHomoOrdinaire() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);

		LOGGER.debug("Test de traitement d'un événement d'union homo.");

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Individu individu = serviceCivil.getIndividu(UNI_HOMO, date(2007, 12, 31));
				Individu conjoint = serviceCivil.getIndividu(UNI_HOMO_CONJOINT, date(2007, 12, 31));
				Mariage mariage = createValidMariage(individu, conjoint, DATE_UNION_HOMO);

				mariage.validate(collector, collector);
				mariage.handle(collector);
				return null;
			}
		});

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertFalse("Une erreur est survenue lors du traitement de l'union homo", collector.hasErreurs());

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

	@Test
	public void testEvenementMariageRedondant() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariage = date(2005, 5, 5);

		// création d'un ménage-commun marié au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariage);
			}
		});

		// création d'un ménage-commun marié au fiscal
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				return null;
			}
		});

		// traitement de l'événement de mariage redondant
		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Individu monsieur = serviceCivil.getIndividu(noMonsieur, dateMariage);
				final Individu madame = serviceCivil.getIndividu(noMadame, dateMariage);
				final Mariage mariage = createValidMariage(monsieur, madame, dateMariage);

				final MessageCollector collector = buildMessageCollector();
				mariage.validate(collector, collector);
				final HandleStatus etat = mariage.handle(collector);

				assertEmpty(collector.getErreurs());
				assertEmpty(collector.getWarnings());
				assertEquals(HandleStatus.REDONDANT, etat);
				return null;
			}
		});

		// on s'assure que rien n'a changé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
				assertNotNull(monsieur);

				final PersonnePhysique madame = tiersService.getPersonnePhysiqueByNumeroIndividu(noMadame);
				assertNotNull(madame);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(madame, dateMariage);
				assertNotNull(ensemble);
				assertSame(monsieur, ensemble.getPrincipal());
				assertSame(madame, ensemble.getConjoint());
				return null;
			}
		});
	}

	@Test
	public void testEvenementMariageJustePasRedondant() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateMariageCivil = date(2005, 5, 5);
		final RegDate dateMariageFiscal = date(2005, 5, 4);

		// création d'un ménage-commun marié au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				marieIndividus(monsieur, madame, dateMariageCivil);
			}
		});
		
		class Ids {
			long monsieur;
			long madame;
			long menage;
		}
		final Ids ids = new Ids();

		// création d'un ménage-commun marié au fiscal
		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				addForPrincipal(monsieur, date(1943, 2, 12), MotifFor.MAJORITE, dateMariageFiscal.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);

				final PersonnePhysique madame = addHabitant(noMadame);
				addForPrincipal(madame, date(1992, 8, 1), MotifFor.MAJORITE, dateMariageFiscal.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Chamblon);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariageFiscal, null);
				addForPrincipal(ensemble.getMenage(), dateMariageFiscal, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				
				ids.monsieur = monsieur.getNumero();
				ids.madame = madame.getNumero();
				ids.menage = ensemble.getMenage().getNumero();
				return null;
			}
		});

		// traitement de l'événement de mariage
		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Individu monsieur = serviceCivil.getIndividu(noMonsieur, dateMariageCivil);
				final Individu madame = serviceCivil.getIndividu(noMadame, dateMariageCivil);
				final Mariage mariage = createValidMariage(monsieur, madame, dateMariageCivil);

				final MessageCollector collector = buildMessageCollector();
				mariage.validate(collector, collector);
				assertEmpty(collector.getWarnings());

				final List<MessageCollector.Msg> erreurs = collector.getErreurs();
				assertNotNull(erreurs);
				assertEquals(2, erreurs.size());
				assertEquals("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(ids.monsieur) + " appartient déjà au ménage commun n° " + FormatNumeroHelper.numeroCTBToDisplay(ids.menage) +
						" en date du 05.05.2005", erreurs.get(0).getMessage());
				assertEquals("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(ids.madame) + " appartient déjà au ménage commun n° " + FormatNumeroHelper.numeroCTBToDisplay(ids.menage) +
						" en date du 05.05.2005", erreurs.get(1).getMessage());
				return null;
			}
		});
	}
	
	private Mariage createValidMariage(Individu individu, Individu conjoint, RegDate dateMariage) {
		return new Mariage(individu, conjoint, dateMariage, 5586, context);
	}

}
