package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(ObtentionPermisTest.class);

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
		ObtentionPermis obtentionPermis = createValidObtentionPermis(celibataire, DATE_OBTENTION_PERMIS, MockCommune.Lausanne.getNoOFS(), 5586);

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

	/**
	 * <b>[SIFISC-9211]</b> même si le permis est obtenu le premier jour d'un mois, l'assujetissement source doit quand-même rester valide tout le mois<br/>
	 * <b>[SIFISC-10518]</b> cela reste vrai, mais pas par le même biais : avant 2014, c'est le for qui était décalé d'un jour, alors que dès 2014, c'est
	 * le calcul de l'assujettissement qui prendra le cas en charge
	 */
	@Test
	public void testObtentionPermisHandlerPremierJourDuMoisAvant2014() throws Exception {

		final long noIndividu = 478423L;
		final RegDate dateNaissance = date(1980, 10, 25);
		final RegDate dateArrivee = date(2000, 5, 12);
		final RegDate dateObtentionPermis = date(2005, 7, 1);

		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");

		// mise en place civile : étranger résident depuis plusieurs années lorsqu'il reçoit le permis C un premier jour de mois
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Oulianov", "Wladimir", Sexe.MASCULIN);
				addPermis(individu, TypePermis.SEJOUR, dateArrivee, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(individu, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
				addNationalite(individu, MockPays.Russie, dateNaissance, null);
			}
		});

		// mise en place fiscale : for source depuis l'arrivée
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.ChateauDoex, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Individu ind = serviceCivil.getIndividu(noIndividu, null);
			final ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(ind, dateObtentionPermis, MockCommune.ChateauDoex.getNoOFS(), TypePermis.ETABLISSEMENT);

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			obtentionPermis.handle(collector);

			assertEmpty(collector.getErreurs());
			assertEmpty(collector.getWarnings());
			return null;
		});

		// vérification de l'état des fors du contribuable
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			assertEquals((Integer) MockCommune.ChateauDoex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			assertNull(ffp.getDateFin());
			assertEquals(dateObtentionPermis.getOneDayAfter(), ffp.getDateDebut());

			final List<Assujettissement> assujettissement = assujettissementService.determine(pp);
			assertNotNull(assujettissement);
			assertEquals(2, assujettissement.size());
			{
				final Assujettissement ass = assujettissement.get(0);
				assertNotNull(ass);
				assertInstanceOf(SourcierPur.class, ass);
				assertEquals(dateArrivee, ass.getDateDebut());
				assertEquals(dateObtentionPermis.getLastDayOfTheMonth(), ass.getDateFin());
			}
			{
				final Assujettissement ass = assujettissement.get(1);
				assertNotNull(ass);
				assertInstanceOf(VaudoisOrdinaire.class, ass);
				assertEquals(dateObtentionPermis.getLastDayOfTheMonth().getOneDayAfter(), ass.getDateDebut());
				assertNull(ass.getDateFin());
			}
			return null;
		});
	}

	/**
	 * <b>[SIFISC-9211]</b> même si le permis est obtenu le premier jour d'un mois, l'assujetissement source doit quand-même rester valide tout le mois<br/>
	 * <b>[SIFISC-10518]</b> cela reste vrai, mais pas par le même biais : avant 2014, c'est le for qui était décalé d'un jour, alors que dès 2014, c'est
	 * le calcul de l'assujettissement qui prendra le cas en charge
	 */
	@Test
	public void testObtentionPermisHandlerPremierJourDuMoisDes2014() throws Exception {

		final long noIndividu = 478423L;
		final RegDate dateNaissance = date(1980, 10, 25);
		final RegDate dateArrivee = date(2000, 5, 12);
		final RegDate dateObtentionPermis = date(2014, 1, 1);

		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");

		// ce test a été lancé en développement avec un décalage temporel (voir la variable d'environnement DateConstants.TIME_OFFSET)
		// en intégration continue, il ne tournera de manière significative qu'une fois l'année 2014 effectivement commencée !!
		if (RegDate.get().isAfterOrEqual(dateObtentionPermis)) {

			// mise en place civile : étranger résident depuis plusieurs années lorsqu'il reçoit le permis C un premier jour de mois
			serviceCivil.setUp(new MockServiceCivil() {
				@Override
				protected void init() {
					final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Oulianov", "Wladimir", Sexe.MASCULIN);
					addPermis(individu, TypePermis.SEJOUR, dateArrivee, dateObtentionPermis.getOneDayBefore(), false);
					addPermis(individu, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
					addNationalite(individu, MockPays.Russie, dateNaissance, null);
				}
			});

			// mise en place fiscale : for source depuis l'arrivée
			final long ppId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.ChateauDoex, ModeImposition.SOURCE);
				return pp.getNumero();
			});

			doInNewTransactionAndSession(status -> {
				final Individu ind = serviceCivil.getIndividu(noIndividu, null);
				final ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(ind, dateObtentionPermis, MockCommune.ChateauDoex.getNoOFS(), TypePermis.ETABLISSEMENT);

				final MessageCollector collector = buildMessageCollector();
				obtentionPermis.validate(collector, collector);
				obtentionPermis.handle(collector);

				assertEmpty(collector.getErreurs());
				assertEmpty(collector.getWarnings());
				return null;
			});

			// vérification de l'état des fors du contribuable
			doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.ChateauDoex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				assertNull(ffp.getDateFin());
				assertEquals(dateObtentionPermis, ffp.getDateDebut());

				final List<Assujettissement> assujettissement = assujettissementService.determine(pp);
				assertNotNull(assujettissement);
				assertEquals(2, assujettissement.size());
				{
					final Assujettissement ass = assujettissement.get(0);
					assertNotNull(ass);
					assertInstanceOf(SourcierPur.class, ass);
					assertEquals(dateArrivee, ass.getDateDebut());
					assertEquals(dateObtentionPermis.getLastDayOfTheMonth(), ass.getDateFin());
				}
				{
					final Assujettissement ass = assujettissement.get(1);
					assertNotNull(ass);
					assertInstanceOf(VaudoisOrdinaire.class, ass);
					assertEquals(dateObtentionPermis.getLastDayOfTheMonth().getOneDayAfter(), ass.getDateDebut());
					assertNull(ass.getDateFin());
				}
				return null;
			});
		}
	}

	/**
	 * Avant 2014, on ne pouvait pas traiter une obtention de permis C / nationalité suisse le jour de son obtention, mais depuis 2014, on peut
	 */
	@Test
	public void testTraitementObtentionDuJour() throws Exception {
		final long noIndividu = 478423L;
		final RegDate dateNaissance = date(1980, 10, 25);
		final RegDate dateObtentionPermis = RegDate.get();
		final RegDate dateArrivee = dateObtentionPermis.addYears(-5);

		// mise en place civile : étranger résident depuis plusieurs années lorsqu'il reçoit le permis C aujourd'hui
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Oulianov", "Wladimir", Sexe.MASCULIN);
				addPermis(individu, TypePermis.SEJOUR, dateArrivee, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(individu, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
				addNationalite(individu, MockPays.Russie, dateNaissance, null);
			}
		});

		// mise en place fiscale : for source depuis l'arrivée
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.ChateauDoex, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Individu ind = serviceCivil.getIndividu(noIndividu, null);
			final ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(ind, dateObtentionPermis, MockCommune.ChateauDoex.getNoOFS(), TypePermis.ETABLISSEMENT);

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertEmpty(collector.getWarnings());
			assertEmpty(collector.getErreurs());

			obtentionPermis.handle(collector);
			assertEmpty(collector.getWarnings());
			assertEmpty(collector.getErreurs());
			return null;
		});

		// vérification de l'état des fors du contribuable
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			assertEquals((Integer) MockCommune.ChateauDoex.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			assertNull(ffp.getDateFin());
			assertEquals(dateObtentionPermis, ffp.getDateDebut());
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionPermisHandlerSourcierMarieSeul() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié seul.");
		Individu marieSeul = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE_SEUL, date(2007, 12, 31));
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieSeul, DATE_OBTENTION_PERMIS, MockCommune.Lausanne.getNoOFS(), 5586);

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
		ForFiscalPrincipalPP forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun", forCommun);
		assertEquals("La date d'ouverture du nouveau for ne correspond pas au lendemain de la date de l'obtention du permis", DATE_OBTENTION_PERMIS.getOneDayAfter(), forCommun.getDateDebut());
		assertEquals(ModeImposition.ORDINAIRE, forCommun.getModeImposition());

		assertEquals(date(1986, 5, 1), menageCommun.getReindexOn()); // [UNIREG-1979] permis C -> réindexation au début du mois suivant l'obtention
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testObtentionPermisHandlerSourcierMarieADeux() throws Exception {

		setupServiceCivilAndLoadDatabase();

		LOGGER.debug("Test de traitement d'un événement d'obtention de permis de marié à deux.");
		Individu marieADeux = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_MARIE, date(2007, 12, 31));
		ObtentionPermis obtentionPermis = createValidObtentionPermis(marieADeux, DATE_OBTENTION_PERMIS, MockCommune.Lausanne.getNoOFS(), 5586);

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
		final ForFiscalPrincipalPP forCommun = menageCommun.getForFiscalPrincipalAt(null);
		assertNotNull("Aucun for fiscal principal trouvé sur le tiers MenageCommun",  forCommun);
		assertEquals("La date d'ouverture du nouveau for ne correspond pas au lendemain de la date de l'obtention du permis", DATE_OBTENTION_PERMIS.getOneDayAfter(), forCommun.getDateDebut());
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
				addPermis(julie, TypePermis.SEJOUR, dateNaissance, dateObtentionPermis.getOneDayBefore(), false);
				addPermis(julie, TypePermis.ETABLISSEMENT, dateObtentionPermis, null, false);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepart, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
			pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
			assertNull(pp.getCategorieEtranger());
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
			final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Lausanne.getNoOFS(), MockCommune.Neuchatel.getNoOFS());

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			assertFalse(pp.isHabitantVD());
			assertEquals(CategorieEtranger._03_ETABLI_C, pp.getCategorieEtranger());
			return null;
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
				addPermis(julie, TypePermis.SEJOUR, dateObtentionPermis, null, false);
				addAdresse(julie, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepart, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
			pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
			assertNull(pp.getCategorieEtranger());
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
			final ObtentionPermis obtentionPermis = createValidObtentionPermisNonC(julie, dateObtentionPermis, MockCommune.Neuchatel.getNoOFS(), TypePermis.SEJOUR);

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			return null;
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			assertFalse(pp.isHabitantVD());
			assertEquals(CategorieEtranger._02_PERMIS_SEJOUR_B, pp.getCategorieEtranger());
			return null;
		});
	}

	/**
	 * [SIFISC-1199] Vérifie que l'obtention d'un permis C et le passage du mode d'imposition source à ordinaire provoque bien la réindexation dans le futur (= à la fin du mois) du contribuable.
	 */
	@Test
	public void testDateReindexationSuiteObtentionPermisEtablissementSourcier() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateArrivee = date(2002, 1, 1);
		final RegDate today = RegDate.get();
		final RegDate dateObtentionPermis = today.getOneDayBefore();
		final RegDate dateDebutMoisProchain = dateObtentionPermis.getLastDayOfTheMonth().getOneDayAfter();

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

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
			pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			assertNull(pp.getCategorieEtranger());
			assertNull(pp.getReindexOn());
			return pp.getNumero();
		});

		// Traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(status -> {
			final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
			final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Lausanne.getNoOFS(), MockCommune.Lausanne.getNoOFS());

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			return null;
		});

		// On vérifie que le tiers est flaggé comme devant être réindexé au 1er du mois suivant
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			assertEquals(dateDebutMoisProchain, pp.getReindexOn()); // [SIFISC-1199] date de réindexation dans le futur car il y a une de transition source -> ordinaire
			if (dateObtentionPermis.getOneDayAfter().day() == 1) {
				assertEquals("Imposition ordinaire VD", tiersService.getRoleAssujettissement(pp, today));
			}
			else {
				assertEquals("Imposition à la source", tiersService.getRoleAssujettissement(pp, today));
			}
			return null;
		});
	}

	/**
	 * [SIFISC-1199] Vérifie que l'obtention d'un permis C sur un contribuable non-assujetti (sourcier implicite( provoque bien la réindexation dans le futur (= à la fin du mois) du contribuable.
	 */
	@Test
	public void testDateReindexationSuiteObtentionPermisEtablissementSourcierImplicite() throws Exception {

		final RegDate dateNaissance = date(1977, 5, 23);
		final RegDate dateArrivee = date(2002, 1, 1);
		final RegDate today = RegDate.get();
		final RegDate dateObtentionPermis = today.getOneDayBefore();
		final RegDate dateDebutMoisProchain = dateObtentionPermis.getLastDayOfTheMonth().getOneDayAfter();

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

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Julie", "Goux", dateNaissance, Sexe.FEMININ);
			pp.setNumeroIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE);
			assertNull(pp.getCategorieEtranger());
			assertNull(pp.getReindexOn());
			return pp.getNumero();
		});

		// Traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(status -> {
			final Individu julie = serviceCivil.getIndividu(NO_INDIVIDU_SOURCIER_CELIBATAIRE, date(2007, 12, 31));
			final ObtentionPermis obtentionPermis = createValidObtentionPermis(julie, dateObtentionPermis, MockCommune.Lausanne.getNoOFS(), MockCommune.Lausanne.getNoOFS());

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			return null;
		});

		// On vérifie que le tiers est flaggé comme devant être réindexé au 1er du mois suivant
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);
			// [SIFISC-1199] date de réindexation dans le futur car il y a une de transition source -> ordinaire (le calcul de l'assujettissement détermine
			// que le contribuable était sourcier à cause du mode d'ouverture 'obtention de permis C', même s'il n'y a pas de for principal source explicite)
			assertEquals(dateDebutMoisProchain, pp.getReindexOn());
			if (dateObtentionPermis.getOneDayAfter().day() == 1) {
				assertEquals("Imposition ordinaire VD", tiersService.getRoleAssujettissement(pp, today));
			}
			else {
				assertEquals("Imposition à la source", tiersService.getRoleAssujettissement(pp, today));
			}
			return null;
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
				individu.setPermis(new MockPermis(dateDebut, null, null, TypePermis.SEJOUR));
			}
		});
		
		// mise en place fiscale (juste la création du tiers, qui n'a pas de for vaudois)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});
		
		// obtention du permis C
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPermis(new MockPermis(datePermisC, null, null, TypePermis.ETABLISSEMENT));
			}
		});
		
		// traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(noIndividu, null);
			final ObtentionPermis obtentionPermis = createValidObtentionPermis(individu, datePermisC, MockCommune.Echallens.getNoOFS(), 0);   // 0 car l'adresse principale est HC (voir constructeurs ObtentionPermis)

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			final HandleStatus evStatus = obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			assertEquals(HandleStatus.TRAITE, evStatus);
			return null;
		});
		
		// vérification que le for a bougé (= passé à l'ordinaire)
		doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(datePermisC.getOneDayAfter(), ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
			assertEquals(MockCommune.Geneve.getNoOFS(), (long) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture());
			return null;
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
				individu.setPermis(new MockPermis(dateDebut, null, null, TypePermis.SEJOUR));
			}
		});

		// mise en place fiscale (juste la création du tiers)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// obtention du permis C
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPermis(new MockPermis(datePermisC, null, null, TypePermis.ETABLISSEMENT));
			}
		});

		// traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(noIndividu, null);
			final ObtentionPermis obtentionPermis = createValidObtentionPermis(individu, datePermisC, MockCommune.Echallens.getNoOFS(), 0);   // 0 car l'adresse principale est HC (voir constructeurs ObtentionPermis)

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			final HandleStatus evStatus = obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			assertEquals(HandleStatus.TRAITE, evStatus);
			return null;
		});

		// vérification qu'aucun for n'a été créé
		doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			assertNull(ffp);
			return null;
		});
	}

	/**
	 * SIFISC-4535 : cas d'un sourcier en secondaire avec un for principal vaudois (= cas bizarre) qui reçoit le permis C
	 * --> aujourd'hui, unireg passer le sourcier à l'ordinaire (rattachement domicile), ce qui est faux puisque le contribuable
	 * n'est présent qu'en secondaire
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
				individu.setPermis(new MockPermis(dateDebut, null, null, TypePermis.SEJOUR));
			}
		});

		// mise en place fiscale (création du tiers avec for vaudois)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		// obtention du permis C
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPermis(new MockPermis(datePermisC, null, null, TypePermis.ETABLISSEMENT));
			}
		});

		// traitement de l'événement d'obtention de permis d'établissement
		doInNewTransactionAndSession(status -> {
			final Individu individu = serviceCivil.getIndividu(noIndividu, null);
			final ObtentionPermis obtentionPermis = createValidObtentionPermis(individu, datePermisC, MockCommune.Echallens.getNoOFS(), 0);   // 0 car l'adresse principale est HC (voir constructeurs ObtentionPermis)

			final MessageCollector collector = buildMessageCollector();
			obtentionPermis.validate(collector, collector);
			assertFalse(collector.hasErreurs());
			assertFalse(collector.hasWarnings());

			final HandleStatus evStatus = obtentionPermis.handle(collector);
			assertFalse(collector.hasWarnings());
			assertEquals(HandleStatus.TRAITE, evStatus);
			return null;
		});

		// vérification que le for a bougé (= passé à l'ordinaire)
		doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			assertNotNull(pp);

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(datePermisC.getOneDayAfter(), ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			assertEquals(MockCommune.Echallens.getNoOFS(), (long) ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}
}
