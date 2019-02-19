package ch.vd.unireg.metier;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.OuvertureForsResults.Erreur;
import ch.vd.unireg.metier.OuvertureForsResults.Traite;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.validation.fors.ForFiscalValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OuvertureForsContribuablesMajeursProcessorTest extends BusinessTest {

	private OuvertureForsContribuablesMajeursProcessor processor;
	//Mock permettant de lever des exceptions de traitements
	private MockOuvertureForsContribuablesMajeursProcessor mockProcessor;
	private AdresseService adresseService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		adresseService = getBean(AdresseService.class, "adresseService");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");


		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new OuvertureForsContribuablesMajeursProcessor(transactionManager, hibernateTemplate, tiersDAO, tiersService, adresseService,
				serviceInfra, serviceCivilCacheWarmer, validationService);

		mockProcessor = new MockOuvertureForsContribuablesMajeursProcessor(transactionManager, hibernateTemplate, tiersDAO, tiersService, adresseService,
				serviceInfra, serviceCivilCacheWarmer, validationService);
	}

	/**
	 * Vérifie que le batch n'ouvre pas de for principal sur un habitant suisse qui n'est pas encore majeur.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseNonMajeur() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(2000, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.habitantTraites);

		final List<OuvertureForsResults.Ignore> ignores = rapport.contribuablesIgnores;
		assertEquals(1, ignores.size());
		final OuvertureForsResults.Ignore ignore = ignores.get(0);
		assertNotNull(ignores);
		assertEquals(OuvertureForsResults.IgnoreType.MINEUR, ignore.getRaison());

		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.principauxPP);
		assertEmpty(fors.principauxPM);
		assertEmpty(fors.secondaires);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
	}

	/**
	 * Vérifie que le batch ouvre bien un for principal ordinaire sur un habitant suisse majeur qui n'en possède pas.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseMajeurSansFor() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.ORDINAIRE, traite.modeImposition);

		// Vérification qu'un for a bien été ouvert sur le contribuable
		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
		assertEmpty(fors.secondaires);
		assertEmpty(fors.principauxPM);

		final List<ForFiscalPrincipalPP> principaux = fors.principauxPP;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipalPP fp = principaux.get(0);
		assertNotNull(fp);
		assertEquals(dateNaissance.addYears(18), fp.getDateDebut());
		assertNull(fp.getDateFin());
		assertEquals(ModeImposition.ORDINAIRE, fp.getModeImposition());
		assertEquals(MotifRattachement.DOMICILE, fp.getMotifRattachement());
		assertEquals(MockCommune.Lausanne.getNoOFS(), fp.getNumeroOfsAutoriteFiscale().intValue());
	}


	/**
	 * Vérifie que le rapport du batch contient bien le numéro d'OID calculé à partir du for nouvellement crée .
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantMajeurSansForAvecOID() throws Exception {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);
		class Ids {
				long jean;
			}
			final Ids ids = new Ids();


		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final OuvertureForsResults rapport = doInNewTransaction(new TxCallback<OuvertureForsResults>() {
			@Override
			public OuvertureForsResults execute(TransactionStatus status) throws Exception {
				final PersonnePhysique h = addHabitant(noIndividu);
				h.setOfficeImpotId(18);
				ids.jean = h.getNumero();
				// Lancement du batch
				final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
				processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);
				return rapport;
			}
		});


		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);
		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());
		final Traite traite = traites.get(0);
		assertEquals((Integer) 7, traite.officeImpotID);

	}

	/**
	 * Vérifie que le batch ouvre bien un for principal ordinaire sur un habitant suisse majeur qui n'en possède pas.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantEtrangerMajeurSansFor() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Tetram", "Ducik", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateNaissance, null);
				addNationalite(individu, MockPays.Albanie, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.SOURCE, traite.modeImposition);

		// Vérification qu'un for a bien été ouvert sur le contribuable
		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
		assertEmpty(fors.secondaires);
		assertEmpty(fors.principauxPM);

		final List<ForFiscalPrincipalPP> principaux = fors.principauxPP;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipalPP fp = principaux.get(0);
		assertNotNull(fp);
		assertEquals(dateNaissance.addYears(18), fp.getDateDebut());
		assertNull(fp.getDateFin());
		assertEquals(MotifRattachement.DOMICILE, fp.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, fp.getModeImposition());
		assertEquals(MockCommune.Lausanne.getNoOFS(), fp.getNumeroOfsAutoriteFiscale().intValue());
	}

	/**
	 * Vérifie que le batch n'ouvre pas de for supplémentaire sur un habitant majeur qui en possède déjà.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseMajeurAvecForPreexistant() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);
		addForPrincipal(h, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		assertEmpty(rapport.habitantEnErrors);

		final List<OuvertureForsResults.Ignore> ignores = rapport.contribuablesIgnores;
		assertEquals(1, ignores.size());

		final OuvertureForsResults.Ignore i = ignores.get(0);
		assertNotNull(i);
		// le for existe déjà (voir remarque dans javadoc du test)
		assertEquals(OuvertureForsResults.IgnoreType.FOR_PRINCIPAL_EXISTANT, i.raison);
	}

	/**
	 * Vérifie que le batch génère une erreur lorsqu'un habitant ne possède pas d'adresse de domicile (les valeurs par défaut ne doivent pas
	 * être utilisées).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseMajeurSansAdresseDomicile() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				// adresse courrier seulement -> l'adresse de domicile sera une adresse par défaut
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		assertEmpty(rapport.habitantEnErrors);

		final List<OuvertureForsResults.Ignore> ignores = rapport.contribuablesIgnores;
		assertEquals(1, ignores.size());

		final OuvertureForsResults.Ignore i = ignores.get(0);
		assertNotNull(i);
		assertEquals(OuvertureForsResults.IgnoreType.ADRESSE_DOMICILE_EST_DEFAUT, i.raison);
	}

	/**
	 * [UNIREG-1588] Vérifie que le batch génère une erreur lorsqu'un habitant ne possède pas de nationalité.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseMajeurSansNationalite() {

		final int noIndividu1 = 1234;
		final int noIndividu2 = 4321;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un individu sans nationalité
				MockIndividu individu1 = addIndividu(noIndividu1, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateNaissance, null);
				individu1.setNationalites(null);

				// un autre individu sans nationalité (variante)
				MockIndividu individu2 = addIndividu(noIndividu2, dateNaissance, "Schmoledu", "Jean", true);
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateNaissance, null);
				individu2.setNationalites(Collections.<Nationalite>emptyList());
			}
		});

		final PersonnePhysique h1 = addHabitant(noIndividu1);
		final PersonnePhysique h2 = addHabitant(noIndividu2);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h1.getNumero(), dateTraitement, rapport);
		processor.traiteHabitant(h2.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(2, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		assertEmpty(rapport.contribuablesIgnores);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(2, erreurs.size());

		final Erreur e1 = erreurs.get(0);
		assertNotNull(e1);
		assertEquals(OuvertureForsResults.ErreurType.CIVIL_EXCEPTION, e1.raison);
		assertEquals("ch.vd.unireg.tiers.TiersException: Impossible de déterminer la nationalité de l'individu n°" + noIndividu1, e1.details);

		final Erreur e2 = erreurs.get(1);
		assertNotNull(e2);
		assertEquals(OuvertureForsResults.ErreurType.CIVIL_EXCEPTION, e2.raison);
		assertEquals("ch.vd.unireg.tiers.TiersException: Impossible de déterminer la nationalité de l'individu n°" + noIndividu2, e2.details);
	}

		/**
	 * [SIFISC-488] Message d'erreur plus parlant
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMessageException() {

		final int noIndividu1 = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un individu sans nationalité
				MockIndividu individu1 = addIndividu(noIndividu1, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu1, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateNaissance, null);
				individu1.setNationalites(null);

			}
		});

		final PersonnePhysique h1 = addHabitant(noIndividu1);


		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		mockProcessor.traiteHabitant(h1.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		assertEmpty(rapport.contribuablesIgnores);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(2, erreurs.size());

		final Erreur e1 = erreurs.get(0);
		assertNotNull(e1);
		assertEquals(OuvertureForsResults.ErreurType.INFRA_EXCEPTION, e1.raison);

		final Erreur e2 = erreurs.get(1);
		assertNotNull(e2);
		assertEquals(OuvertureForsResults.ErreurType.CONSTRAINT_VIOLATION_EXCEPTION, e2.raison);
	}

	/**
	 * [UNIREG-1585] Vérifie que le batch renseigne bien l'office d'impôt, notamment sur les contribuables qui ne possèdaient pas de for fiscal avant
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRenseignementOIDapresTraitement() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.ORDINAIRE, traite.modeImposition);
		assertNotNull(traite.officeImpotID);
		assertEquals(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), traite.officeImpotID.intValue());
	}

	/**
	 * [UNIREG-3379] Vérifie que le batch ouvre bien un for principal ordinaire sur la bonne commune pour un habitant suisse majeur qui habitent une commune fusionnée au civil mais pas au fiscal.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseMajeurSansForSurCommuneFusionneeAuCivilMaisPasAuFiscal() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2010, 11, 1);
		final RegDate dateNaissance = date(1992, 10, 17); // => majeur le 17 octobre 2010

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.Gressy.BatimentLesPechauds, null, null, dateNaissance, null); // localité Gressy => commune de Gressy jusqu'à fin 2010
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Gressy est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Gressy.getDateFinValidite().addMonths(-1));
		try {
			processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.ORDINAIRE, traite.modeImposition);

		// Vérification qu'un for a bien été ouvert sur le contribuable
		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
		assertEmpty(fors.secondaires);
		assertEmpty(fors.principauxPM);

		final List<ForFiscalPrincipalPP> principaux = fors.principauxPP;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipalPP fp = principaux.get(0);
		assertForPrincipal(dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Gressy, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fp);
	}

	/**
	 * [UNIREG-3379] Vérifie que le batch ouvre bien un for principal ordinaire sur la bonne commune pour un habitant suisse majeur qui habitent une commune fusionnée au civil et au fiscal.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiteHabitantSuisseMajeurSansForSurCommuneFusionneeAuCivilEtAuFiscal() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2011, 2, 1);
		final RegDate dateNaissance = date(1993, 1, 17); // => majeur le 17 janvier 2011

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.Gressy.BatimentLesPechauds, null, null, dateNaissance, null); // localité Gressy => commune d'Yverdon-les-Bains dès 2011
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.traiteHabitant(h.getNumero(), dateTraitement, rapport);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.ORDINAIRE, traite.modeImposition);

		// Vérification qu'un for a bien été ouvert sur le contribuable
		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
		assertEmpty(fors.secondaires);
		assertEmpty(fors.principauxPM);

		final List<ForFiscalPrincipalPP> principaux = fors.principauxPP;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipalPP fp = principaux.get(0);
		assertForPrincipal(dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.YverdonLesBains, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fp);
	}

	/**
	 * [SIFISC-9238] C'est la nationalité à la date de la majorité qui doit déterminer le for créé
	 */
	@Test
	public void testHabitantMajeurLaVeilleDeSaNationaliteSuisse() throws Exception {

		final RegDate dateNaissance = date(1990, 7, 23);
		final RegDate dateMajorite = dateNaissance.addYears(18);
		final RegDate dateArrivee = dateMajorite.addYears(-2);
		final RegDate dateNationalite = dateMajorite.addDays(1);
		final long noIndividu = 32784326L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Potter", "Harry", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null);
				addNationalite(ind, MockPays.Suisse, dateNationalite, null);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// majorisation  !
		final OuvertureForsResults rapport = processor.run(RegDate.get(), null);
		assertNotNull(rapport);
		assertEquals(1, rapport.habitantTraites.size());
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final Traite traite = rapport.habitantTraites.get(0);
		assertNotNull(traite);
		assertEquals(ppId, traite.noCtb);
		assertEquals(ModeImposition.SOURCE, traite.modeImposition);

		// vérification du for créé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateMajorite, ffp.getDateDebut());
				assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-9238] C'est l'adresse de résidence à la date de la majorité qui doit déterminer le for créé
	 */
	@Test
	public void testHabitantMajeurLaVeilleDunDemenagement() throws Exception {

		final RegDate dateNaissance = date(1990, 7, 23);
		final RegDate dateMajorite = dateNaissance.addYears(18);
		final RegDate dateDemenagement = dateMajorite.addDays(1);
		final long noIndividu = 32784326L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Tell", "William", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, dateDemenagement);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateDemenagement.getOneDayAfter(), null);
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// majorisation  !
		final OuvertureForsResults rapport = processor.run(RegDate.get(), null);
		assertNotNull(rapport);
		assertEquals(1, rapport.habitantTraites.size());
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final Traite traite = rapport.habitantTraites.get(0);
		assertNotNull(traite);
		assertEquals(ppId, traite.noCtb);
		assertEquals(ModeImposition.ORDINAIRE, traite.modeImposition);

		// vérification du for créé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateMajorite, ffp.getDateDebut());
				assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());
				assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());    // <-- il était à Echallens pour son anniversaire, même s'il est parti juste après
				return null;
			}
		});
	}

	/**
	 * SIFISC-19003 : cas de l'arrivée HS dans l'année de majorité (avant la majorité)
	 */
	@Test
	public void testArriveeHorsSuisseAvantMajoriteMemeAnnee() throws Exception {

		final long noIndividu = 48161815L;
		final int anneeMajorite = 2016;
		final RegDate dateMajorite = date(anneeMajorite, 7, 12);
		final RegDate dateArriveeHorsSuisse = date(anneeMajorite, 6, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = dateMajorite.addYears(-18);
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "de Valombreuse", "Christine", Sexe.FEMININ);
				final MockAdresse adresse = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArriveeHorsSuisse, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				addNationalite(individu, MockPays.France, dateNaissance, null);
				addPermis(individu, TypePermis.SEJOUR, dateArriveeHorsSuisse, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// lancement du job de majorisation
		final OuvertureForsResults rapport = processor.run(RegDate.get(), null);
		assertNotNull(rapport);
		assertEquals(1, rapport.habitantTraites.size());
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final Traite traite = rapport.habitantTraites.get(0);
		assertNotNull(traite);
		assertEquals(ppId, traite.noCtb);
		assertEquals(ModeImposition.SOURCE, traite.modeImposition);
		assertEquals(dateArriveeHorsSuisse, traite.dateOuverture);
		assertEquals(MotifFor.ARRIVEE_HS, traite.motifOuverture);

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertFalse(ffp.isAnnule());
				assertEquals(dateArriveeHorsSuisse, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				assertNull(ffp.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
			}
		});
	}

	/**
	 * SIFISC-19003 : cas de l'arrivée HS l'année d'avant l'année de majorité
	 */
	@Test
	public void testArriveeHorsSuisseAnneeAvantMajorite() throws Exception {

		final long noIndividu = 48161815L;
		final int anneeMajorite = 2016;
		final RegDate dateMajorite = date(anneeMajorite, 7, 12);
		final RegDate dateArriveeHorsSuisse = date(anneeMajorite - 1, 8, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = dateMajorite.addYears(-18);
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "de Valombreuse", "Christine", Sexe.FEMININ);
				final MockAdresse adresse = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArriveeHorsSuisse, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				addNationalite(individu, MockPays.France, dateNaissance, null);
				addPermis(individu, TypePermis.SEJOUR, dateArriveeHorsSuisse, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// lancement du job de majorisation
		final OuvertureForsResults rapport = processor.run(RegDate.get(), null);
		assertNotNull(rapport);
		assertEquals(1, rapport.habitantTraites.size());
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.contribuablesIgnores);

		final Traite traite = rapport.habitantTraites.get(0);
		assertNotNull(traite);
		assertEquals(ppId, traite.noCtb);
		assertEquals(ModeImposition.SOURCE, traite.modeImposition);
		assertEquals(dateMajorite, traite.dateOuverture);
		assertEquals(MotifFor.MAJORITE, traite.motifOuverture);

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertFalse(ffp.isAnnule());
				assertEquals(dateMajorite, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
				assertNull(ffp.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
			}
		});
	}
}
