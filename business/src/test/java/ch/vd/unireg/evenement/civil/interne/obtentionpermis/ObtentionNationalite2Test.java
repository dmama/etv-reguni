package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test du handler des événements d'obtention de nationalité.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionNationalite2Test extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ObtentionNationalite2Test.class);

	/**
	 * Le numéro d'individu celibataire.
	 */
	private static final Long NO_INDIVIDU_SOURCIER_CELIBATAIRE = 6789L;

	/**
	 * Le numéro d'individu marié seul.
	 */
	private static final Long NO_INDIVIDU_SOURCIER_MARIE_SEUL = 12345L;

	/**
	 * Numéros des individus mariés.
	 */
	private static final Long NO_INDIVIDU_SOURCIER_MARIE = 54321L;
	private static final Long NO_INDIVIDU_SOURCIER_MARIE_CONJOINT = 23456L;

	/**
	 * Les dates d'obtention de la nationalité
	 */
	private static final RegDate DATE_OBTENTION_NATIONALITE = RegDate.get(1986, 4, 8);

	/**
	 * Le fichier de données de test. C'est le meme que pour l'obtention de
	 * permis, les règles de traitement similiaires.
	 */
	private static final String DB_UNIT_DATA_FILE = "ObtentionPermisTest.xml";

	private void setupServiceCivilAndLoadDb() throws Exception {
		serviceCivil.setUp(new DefaultMockIndividuConnector());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	/**
	 * @param tiers
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionNationaliteHandlerSourcierCelibataire() throws Exception {

		setupServiceCivilAndLoadDb();

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
		ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(celibataire, DATE_OBTENTION_NATIONALITE, 5586);

		final MessageCollector collector = buildMessageCollector();
		obtentionNationalite.validate(collector, collector);
		obtentionNationalite.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertFalse("Une erreur est survenue lors du traitement d'obtention de nationalité de célibataire.", collector.hasErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique julie = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.assertTrue("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", julie != null);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Julie ont disparu", julie.getForsFiscaux());
		Assert.assertNotNull("Julie devrait encore avoir un for principal actif après l'obtention de nationalité suisse", julie.getForFiscalPrincipalAt(null));
		Assert.assertEquals(ModeImposition.ORDINAIRE, julie.getForFiscalPrincipalAt(null).getModeImposition());
	}

	/**
	 * @param tiers
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionNationaliteHandlerSourcierCelibataireMaisNationaliteNonSuisse() throws Exception {

		setupServiceCivilAndLoadDb();

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité non suisse de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
		ObtentionNationalite obtentionNationalite = createValidObtentionNationaliteNonSuisse(celibataire, DATE_OBTENTION_NATIONALITE, 5586);

		final MessageCollector collector = buildMessageCollector();
		obtentionNationalite.validate(collector, collector);
		obtentionNationalite.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertFalse("Une erreur est survenue lors du traitement d'obtention de nationalité de célibataire.", collector.hasErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", julie);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNotNull("Les for fiscaux de Julie ont disparu", julie.getForsFiscaux());
		Assert.assertNotNull("Julie devrait encore avoir un for principal actif après l'obtention de nationalité non suisse", julie.getForFiscalPrincipalAt(null));
		Assert.assertEquals("Julie devrait encore avoir son for principal actif inchangé après l'obtention de nationalité autre que suisse", ModeImposition.SOURCE, julie.getForFiscalPrincipalAt(null).getModeImposition());
	}

	/**
	 * @param tiers
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionNationaliteHandlerSourcierMarieSeul() throws Exception {

		setupServiceCivilAndLoadDb();

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité de marié seul.");

		final MessageCollector collector = buildMessageCollector();
		doInNewTransaction(status -> {
			Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL, date(2007, 12, 31));
			ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(marieSeul, DATE_OBTENTION_NATIONALITE, 5586);

			obtentionNationalite.validate(collector, collector);
			obtentionNationalite.handle(collector);
			return null;
		});

		/*
		 * Test de la présence d'une erreur
		 */
		assertEmpty("Une erreur est survenue lors du traitement d'obtention de nationalité de marié seul.", collector.getErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		PersonnePhysique pierre = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL);
		assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", pierre);

		/*
		 * Vérification des fors fiscaux
		 */
		assertEquals("Les for fiscaux de Pierre ont disparus", 1, pierre.getForsFiscaux().size());
		assertNull("Pierre ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse", pierre.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : pierre.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : pierre.getRapportsSujet()) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipalPP forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		assertEquals("La date d'ouverture du nouveau for ne correspond pas au lendemain de la date de l'obtention de la nationalité suisse", DATE_OBTENTION_NATIONALITE.getOneDayAfter(), forCommun.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());
	}

	/**
	 * @param tiers
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionNationaliteHandlerSourcierMarieADeux() throws Exception {

		setupServiceCivilAndLoadDb();

		LOGGER.debug("Test de traitement d'un événement d'obtention de nationalité de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE, date(2007, 12, 31));
		ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(marieADeux, DATE_OBTENTION_NATIONALITE, 5586);

		final MessageCollector collector = buildMessageCollector();
		obtentionNationalite.validate(collector, collector);
		obtentionNationalite.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		Assert.assertFalse("Une erreur est survenue lors du traitement d'obtention de nationalité de marié seul.", collector.hasErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", momo);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNull("Momo ne doit toujours pas avoir de for principal actif après l'obtention de nationalité suisse", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non-fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Test de récupération du Conjoint
		 */
		final PersonnePhysique bea = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_CONJOINT);
		Assert.assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", bea);

		/*
		 * Vérification des fors fiscaux
		 */
		Assert.assertNull("Béa ne doit toujours pas avoir de for principal actif après  l'obtention de nationalité suisse", bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				Assert.assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for (RapportEntreTiers rapport : momo.getRapportsSujet()) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		Assert.assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		final ForFiscalPrincipalPP forCommun = menageCommun.getForFiscalPrincipalAt(null);
		Assert.assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		Assert.assertEquals("La date d'ouverture du nouveau for ne correspond pas au lendemain de la date de l'obtention de la nationalité suisse", DATE_OBTENTION_NATIONALITE.getOneDayAfter(), forCommun.getDateDebut());
		Assert.assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());
	}

	private ObtentionNationalite createValidObtentionNationalite(Individu individu, RegDate dateObtentionNationalite, int numeroOfsCommunePrincipale) {
		return new ObtentionNationaliteSuisse(individu, null, dateObtentionNationalite, 5586, numeroOfsCommunePrincipale, context);
	}

	private ObtentionNationalite createValidObtentionNationaliteNonSuisse(Individu individu, RegDate dateObtentionNationalite, int numeroOfsCommunePrincipale) {
		return new ObtentionNationaliteNonSuisse(individu, null, dateObtentionNationalite, 5586, numeroOfsCommunePrincipale, context);
	}

	@Test
	public void testObtentionNationaliteSuisseSurAncienHabitant() throws Exception {

		final RegDate dateNaissance = date(1977, 4, 19);
		final RegDate dateObtentionNationalite = date(2006, 6, 1);

		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, dateObtentionNationalite.getOneDayBefore());
				addNationalite(julie, MockPays.Suisse, dateObtentionNationalite, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2000, 1, 1), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Julie", "Goux", RegDate.get(1977, 4, 19), Sexe.FEMININ);
			pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
			Assert.assertNull(pp.getNumeroOfsNationalite());
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
			final ObtentionNationalite obtentionNationalite = createValidObtentionNationalite(julie, dateObtentionNationalite, MockCommune.Geneve.getNoOFS());

			final MessageCollector collector = buildMessageCollector();
			obtentionNationalite.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			obtentionNationalite.handle(collector);
			assertFalse(collector.hasWarnings());
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);
			Assert.assertFalse(pp.isHabitantVD());
			Assert.assertEquals(Integer.valueOf(ServiceInfrastructureService.noOfsSuisse), pp.getNumeroOfsNationalite());
			return null;
		});
	}

	@Test
	public void testObtentionNationaliteNonSuisseSurAncienHabitant() throws Exception {

		final RegDate dateNaissance = date(1977, 4, 19);
		final RegDate dateObtentionNationalite = date(2006, 6, 1);

		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, dateObtentionNationalite.getOneDayBefore());
				addNationalite(julie, MockPays.Allemagne, dateObtentionNationalite, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2000, 1, 1), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Julie", "Goux", RegDate.get(1977, 4, 19), Sexe.FEMININ);
			pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
			Assert.assertNull(pp.getNumeroOfsNationalite());
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31), AttributeIndividu.NATIONALITES);
			final ObtentionNationalite obtentionNationalite = createValidObtentionNationaliteNonSuisse(julie, dateObtentionNationalite, MockCommune.Geneve.getNoOFS());

			final MessageCollector collector = buildMessageCollector();
			obtentionNationalite.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			obtentionNationalite.handle(collector);
			assertFalse(collector.hasWarnings());
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);
			Assert.assertFalse(pp.isHabitantVD());
			Assert.assertEquals(Integer.valueOf(MockPays.Allemagne.getNoOFS()), pp.getNumeroOfsNationalite());
			return null;
		});
	}

}
