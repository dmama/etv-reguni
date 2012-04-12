package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * @author Ludovic BERTIN <mailto:ludovic.bertin@gmail.com>
 * <a>
 */
@SuppressWarnings({"JavaDoc"})
public class ObtentionPermisTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = Logger.getLogger(ObtentionPermisTest.class);

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
	 * Les dates d'obtention du permis
	 */
	private static final RegDate DATE_OBTENTION_PERMIS = RegDate.get(1986, 4, 8);

	/**
	 * Le fichier de données de test.
	 */
	private static final String DB_UNIT_DATA_FILE = "ObtentionPermisTest.xml";

	private void setupServiceCivilAndLoadDatabase() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());
		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionPermisHandlerSourcierCelibataire() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
		ObtentionPermis obtentionPermis = createValidObtentionPermis(celibataire, DATE_OBTENTION_PERMIS, MockCommune.Lausanne.getNoOFSEtendu(), 5586);

		final MessageCollector collector = buildMessageCollector();
		obtentionPermis.validate(collector, collector);
		obtentionPermis.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		assertFalse("Une erreur est survenue lors du traitement d'obtention de permis de célibataire.", collector.hasErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique julie  = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", julie);

		/*
		 * Vérification des fors fiscaux
		 */
		assertNotNull("Les for fiscaux de Julie ont disparu", julie.getForsFiscaux());
		assertNotNull("Julie devrait encore avoir un for principal actif après l'obtention de permis", julie.getForFiscalPrincipalAt(null));
		assertEquals(ModeImposition.ORDINAIRE, julie.getForFiscalPrincipalAt(null).getModeImposition());

		assertEquals(date(1986, 5, 1), julie.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionPermisHandlerSourcierCelibataireMaisPermisNonC() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de célibataire.");
		Individu celibataire = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
		ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(celibataire, DATE_OBTENTION_PERMIS, 4848, TypePermis.COURTE_DUREE);

		final MessageCollector collector = buildMessageCollector();
		obtentionPermis.validate(collector, collector);
		obtentionPermis.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		assertFalse("Une erreur est survenue lors du traitement d'obtention de permis de célibataire.", collector.hasErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique julie = tiersDAO.getHabitantByNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
		assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", julie);

		/*
		 * Vérification des fors fiscaux
		 */
		assertNotNull("Les for fiscaux de Julie ont disparus", julie.getForsFiscaux());
		assertNotNull("Julie devrait encore avoir un for principal actif après l'obtention de permis", julie.getForFiscalPrincipalAt(null));
		assertEquals("Julie devrait encore son for principal actif inchangé après l'obtention de permis autre que C", ModeImposition.SOURCE, julie.getForFiscalPrincipalAt(null).getModeImposition());

		assertNull(julie.getReindexOn()); // [UNIREG-1979] pas de permis C, pas de réindexation dans le futur
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionPermisHandlerSourcierMarieSeul() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié seul.");
		Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL, date(2007, 12, 31));
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieSeul, DATE_OBTENTION_PERMIS, MockCommune.Lausanne.getNoOFSEtendu(), 5586);

		final MessageCollector collector = buildMessageCollector();
		obtentionPermis.validate(collector, collector);
		obtentionPermis.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		assertEmpty("Une erreur est survenue lors du traitement d'obtention de permis de marié seul.", collector.getErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique pierre = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL);
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
		for ( RapportEntreTiers rapport : pierre.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		// 25-06-2009 (PBO) : selon la spéc la date d'ouverture du nouveau for doit être faite le 1er jour du mois qui suit l'événement
		assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention du permis", DATE_OBTENTION_PERMIS, forCommun.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());

		assertEquals(date(1986, 5, 1), menageCommun.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionPermisHandlerSourcierMarieADeux() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE, date(2007, 12, 31));
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieADeux, DATE_OBTENTION_PERMIS, MockCommune.Lausanne.getNoOFSEtendu(), 5586);

		final MessageCollector collector = buildMessageCollector();
		obtentionPermis.validate(collector, collector);
		obtentionPermis.handle(collector);

		/*
		 * Test de la présence d'une erreur
		 */
		assertFalse("Une erreur est survenue lors du traitement d'obtention de permis de marié seul.", collector.hasErreurs());

		/*
		 * Test de récupération du Tiers
		 */
		final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE);
		assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", momo);

		/*
		 * Vérification des fors fiscaux
		 */
		assertNull("Momo ne doit toujours pas avoir de for principal actif après l'obtention de permis", momo.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : momo.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Test de récupération du Conjoint
		 */
		final PersonnePhysique bea = tiersDAO.getPPByNumeroIndividu(NO_INDIVIDU_SOURCIER_MARIE_CONJOINT);
		assertNotNull("Plusieurs habitants trouvés avec le même numéro individu (ou aucun)", bea);

		/*
		 * Vérification des fors fiscaux
		 */
		assertNull("Béa ne doit toujours pas avoir de for principal actif après  l'obtention de permis",  bea.getForFiscalPrincipalAt(null));
		for (ForFiscal forFiscal : bea.getForsFiscaux()) {
			if (forFiscal instanceof ForFiscalPrincipal)
				assertNotNull("Un for fiscal principal non fermé a été trouvé", forFiscal.getDateFin());
		}

		/*
		 * Vérification de la présence d'un tiers MenageCommun
		 */
		MenageCommun menageCommun = null;
		int nbMenagesCommuns = 0;
		for ( RapportEntreTiers rapport : momo.getRapportsSujet() ) {
			if (rapport.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
				nbMenagesCommuns++;
				menageCommun = (MenageCommun) tiersDAO.get(rapport.getObjetId());
			}
		}
		assertEquals("Plusieurs ou aucun tiers MenageCommun ont été trouvés", 1, nbMenagesCommuns);

		/*
		 * Vérification du for principal du tiers MenageCommun
		 */
		final ForFiscalPrincipal forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",  forCommun);
		// 25-06-2009 (PBO) : selon la spéc la date d'ouverture du nouveau for doit être faite le 1er jour du mois qui suit l'événement
		assertEquals("La date d'ouverture du nouveau for ne correspond pas a la date de l'obtention du permis", DATE_OBTENTION_PERMIS, forCommun.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());

		assertEquals(date(1986, 5, 1), menageCommun.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	private ObtentionPermis createValidObtentionPermis(Individu individu, RegDate dateObtentionPermis, int noOfsCommuneAnnonce, int noOfsCommunePrincipaleVaudoise) {
		return new ObtentionPermis(individu, null, dateObtentionPermis, noOfsCommuneAnnonce, noOfsCommunePrincipaleVaudoise, TypePermis.ETABLISSEMENT, context);
	}

	private ObtentionPermis createValidObtentionPermisNonC(Individu individu, RegDate dateObtentionPermis, int noOfsCommunePrincipale, TypePermis typePermis) {
		return new ObtentionPermis(individu, null, dateObtentionPermis, 4848, noOfsCommunePrincipale, typePermis, context);
	}

	@Test
	public void testObtentionPermisEtablissementPourAncienHabitant() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateObtentionPermis = date(2005, 6, 12);
		final RegDate dateDepart = date(2002, 1, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, null);
				addPermis(julie, TypePermis.ANNUEL, dateNaissance, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(julie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepart, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
				pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
				assertNull(pp.getCategorieEtranger());
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Lausanne.getNoOFSEtendu(), MockCommune.Neuchatel.getNoOFS());

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());

				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse(pp.isHabitantVD());
				assertEquals(CategorieEtranger._03_ETABLI_C, pp.getCategorieEtranger());
				return null;
			}
		});
	}

	@Test
	public void testObtentionPermisNonEtablissementPourAncienHabitant() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateObtentionPermis = date(2005, 6, 12);
		final RegDate dateDepart = date(2002, 1, 1);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, null);
				addPermis(julie, TypePermis.COURTE_DUREE, dateNaissance, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(julie, TypePermis.ANNUEL, dateObtentionPermis, null, false);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepart, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
				pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
				assertNull(pp.getCategorieEtranger());
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
				final ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(julie, dateObtentionPermis, MockCommune.Neuchatel.getNoOFS(), TypePermis.ANNUEL);

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());

				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse(pp.isHabitantVD());
				assertEquals(CategorieEtranger._02_PERMIS_SEJOUR_B, pp.getCategorieEtranger());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1199] Vérifie que l'obtention d'un permis C et le passage du mode d'imposition source à ordinaire provoque bien la réindexation dans le futur (= à la fin du mois) du contribuable.
	 */
	@Test
	public void testDateReindexationSuiteObtentionPermisEtablissementSourcier() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateArrivee = date(2002, 1, 1);
		final RegDate dateObtentionPermis = RegDate.get();
		final RegDate dateDebutMoisProchain = RegDate.get(dateObtentionPermis.year(), dateObtentionPermis.month(), 1).addMonths(1);

		// On crée la situation suivante : contribuable de nationalité française domicilée à Lausanne et recevant un permis d'établissement aujourd'hui
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addPermis(julie, TypePermis.COURTE_DUREE, dateArrivee, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(julie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
				pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.SOURCE);
				assertNull(pp.getCategorieEtranger());
				assertNull(pp.getReindexOn());
				return pp.getNumero();
			}
		});

		// Traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Lausanne.getNoOFSEtendu(), MockCommune.Lausanne.getNoOFS());

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());
				return null;
			}
		});

		// On vérifie que le tiers est flaggé comme devant être réindexé au 1er du mois suivant
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertEquals(dateDebutMoisProchain, pp.getReindexOn()); // [SIFISC-1199] date de réindexation dans le futur car il y a une de transition source -> ordinaire
				if (dateObtentionPermis.day() == 1) {
					assertEquals("Imposition ordinaire VD", tiersService.getRoleAssujettissement(pp, RegDate.get()));
				}
				else {
					assertEquals("Imposition à la source", tiersService.getRoleAssujettissement(pp, RegDate.get()));
				}
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1199] Vérifie que l'obtention d'un permis C sur un contribuable non-assujetti (sourcier implicite( provoque bien la réindexation dans le futur (= à la fin du mois) du contribuable.
	 */
	@Test
	public void testDateReindexationSuiteObtentionPermisEtablissementSourcierImplicite() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateArrivee = date(2002, 1, 1);
		final RegDate dateObtentionPermis = RegDate.get();
		final RegDate dateDebutMoisProchain = RegDate.get(dateObtentionPermis.year(), dateObtentionPermis.month(), 1).addMonths(1);

		// On crée la situation suivante : contribuable de nationalité française domicilée à Lausanne et recevant un permis d'établissement aujourd'hui
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu julie = addIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, dateNaissance, "Goux", "Julie", false);
				addNationalite(julie, MockPays.France, dateNaissance, null);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, null);
				addPermis(julie, TypePermis.COURTE_DUREE, dateArrivee, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(julie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
				pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
				assertNull(pp.getCategorieEtranger());
				assertNull(pp.getReindexOn());
				return pp.getNumero();
			}
		});

		// Traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Lausanne.getNoOFSEtendu(), MockCommune.Lausanne.getNoOFS());

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());
				return null;
			}
		});

		// On vérifie que le tiers est flaggé comme devant être réindexé au 1er du mois suivant
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				// [SIFISC-1199] date de réindexation dans le futur car il y a une de transition source -> ordinaire (le calcul de l'assujettissement détermine
				// que le contribuable était sourcier à cause du mode d'ouverture 'obtention de permis C', même s'il n'y a pas de for principal source explicite)
				assertEquals(dateDebutMoisProchain, pp.getReindexOn());
				if (dateObtentionPermis.day() == 1) {
					assertEquals("Imposition ordinaire VD", tiersService.getRoleAssujettissement(pp, RegDate.get()));
				}
				else {
					assertEquals("Imposition à la source", tiersService.getRoleAssujettissement(pp, RegDate.get()));
				}
				return null;
			}
		});
	}

	/**
	 * SIFISC-4535 : cas d'un sourcier en secondaire sans for principal vaudois (= cas normal) qui reçoit le permis C
	 * --> unireg doit traiter le changement de mode d'imposition sur le fors non-vaudois
	 */
	@Test
	public void testResidentSecondaireSourcierObtientPermisC() throws Exception {
		
		final long noIndividu = 12674543L;
		final RegDate dateNaissance = date(1965, 8, 23);
		final RegDate dateDebut = date(2006, 1, 1);
		final RegDate datePermisC = date(2011, 4, 12);

		// mise en place civile d'un individu en secondaire dans le canton avec un permis B
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Kaderate", "Yamamoto", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDebut, null);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateDebut, null);
				individu.setPermis(Arrays.<Permis>asList(new MockPermis(dateDebut, null, null, TypePermis.ANNUEL)));
			}
		});
		
		// mise en place fiscale (juste la création du tiers, qui n'a pas de for vaudois)
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});
		
		// obtention du permis C
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPermis(Arrays.<Permis>asList(new MockPermis(datePermisC, null, null, TypePermis.ETABLISSEMENT)));
			}
		});
		
		// traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu individu = serviceCivil.getIndividu(noIndividu, null);
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(individu, datePermisC, MockCommune.Echallens.getNoOFSEtendu(), 0);   // 0 car l'adresse principale est HC (voir constructeurs ObtentionPermis)

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				final HandleStatus evStatus = obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());
				assertEquals(HandleStatus.TRAITE, evStatus);
				return null;
			}
		});
		
		// vérification que le for a bougé (= passé à l'ordinaire)
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(datePermisC, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				assertEquals(MockCommune.Geneve.getNoOFSEtendu(), (long) ffp.getNumeroOfsAutoriteFiscale());
				assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture());
				return null;
			}
		});
	}

	/**
	 * SIFISC-4535 : cas d'un sourcier en secondaire sans for principal du tout (= cas normal) qui reçoit le permis C
	 * --> unireg ne devrait rien faire et laisser passer l'événement d'obtention de permis
	 */
	@Test
	public void testResidentSecondaireSansForObtientPermisC() throws Exception {

		final long noIndividu = 12674543L;
		final RegDate dateNaissance = date(1965, 8, 23);
		final RegDate dateDebut = date(2006, 1, 1);
		final RegDate datePermisC = date(2011, 4, 12);

		// mise en place civile d'un individu en secondaire dans le canton avec un permis B
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Kaderate", "Yamamoto", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDebut, null);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateDebut, null);
				individu.setPermis(Arrays.<Permis>asList(new MockPermis(dateDebut, null, null, TypePermis.ANNUEL)));
			}
		});

		// mise en place fiscale (juste la création du tiers)
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// obtention du permis C
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPermis(Arrays.<Permis>asList(new MockPermis(datePermisC, null, null, TypePermis.ETABLISSEMENT)));
			}
		});

		// traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu individu = serviceCivil.getIndividu(noIndividu, null);
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(individu, datePermisC, MockCommune.Echallens.getNoOFSEtendu(), 0);   // 0 car l'adresse principale est HC (voir constructeurs ObtentionPermis)

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				final HandleStatus evStatus = obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());
				assertEquals(HandleStatus.TRAITE, evStatus);
				return null;
			}
		});

		// vérification qu'aucun for n'a été créé
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNull(ffp);
				return null;
			}
		});
	}

	/**
	 * SIFISC-4535 : cas d'un sourcier en secondaire avec un for principal vaudois (= cas bizarre) qui reçoit le permis C
	 * --> aujourd'hui, unireg passer le sourcier à l'ordinaire (rattachement domicile), ce qui est faux puisque le contribuable
	 * n'est présent qu'en secondaire
	 * TODO ce test tient compte du comportement actuel, il faudra le modifier dès qu'une décision aura été prise pour SIFISC-4535
	 */
	@Test
	public void testResidentSecondaireSourcierForVaudoisObtientPermisC() throws Exception {

		final long noIndividu = 12674543L;
		final RegDate dateNaissance = date(1965, 8, 23);
		final RegDate dateDebut = date(2006, 1, 1);
		final RegDate datePermisC = date(2011, 4, 12);

		// mise en place civile d'un individu en secondaire dans le canton avec un permis B
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Kaderate", "Yamamoto", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDebut, null);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.Echallens.GrandRue, null, dateDebut, null);
				individu.setPermis(Arrays.<Permis>asList(new MockPermis(dateDebut, null, null, TypePermis.ANNUEL)));
			}
		});

		// mise en place fiscale (création du tiers avec for vaudois)
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// obtention du permis C
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPermis(Arrays.<Permis>asList(new MockPermis(datePermisC, null, null, TypePermis.ETABLISSEMENT)));
			}
		});

		// traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Individu individu = serviceCivil.getIndividu(noIndividu, null);
				final ObtentionPermis obtentionPermis = createValidObtentionPermis(individu, datePermisC, MockCommune.Echallens.getNoOFSEtendu(), 0);   // 0 car l'adresse principale est HC (voir constructeurs ObtentionPermis)

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());

				final HandleStatus evStatus = obtentionPermis.handle(collector);
				assertFalse(collector.hasWarnings());
				assertEquals(HandleStatus.TRAITE, evStatus);
				return null;
			}
		});

		// vérification que le for a bougé (= passé à l'ordinaire)
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(datePermisC, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (long) ffp.getNumeroOfsAutoriteFiscale());
				return null;
			}
		});
	}
}
