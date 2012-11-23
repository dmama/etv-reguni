package ch.vd.uniregctb.metier;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.OuvertureForsResults.Erreur;
import ch.vd.uniregctb.metier.OuvertureForsResults.Traite;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.validation.ValidationService;
import ch.vd.uniregctb.validation.fors.ForFiscalValidator;

import static org.junit.Assert.assertEquals;
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
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.habitantTraites);

		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.principaux);
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
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

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

		final List<ForFiscalPrincipal> principaux = fors.principaux;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipal fp = principaux.get(0);
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

		final OuvertureForsResults rapport = doInTransaction(new TxCallback<OuvertureForsResults>() {
			@Override
			public OuvertureForsResults execute(TransactionStatus status) throws Exception {
				final PersonnePhysique h = addHabitant(noIndividu);
				h.setOfficeImpotId(18);
				ids.jean = h.getNumero();
				// Lancement du batch
				final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
				processor.rapport = rapport;
				processor.traiteHabitant(h.getNumero(), dateTraitement);
				return rapport;
			}
		});


		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());
		final Traite traite = traites.get(0);
		assertEquals(7, traite.officeImpotID.longValue());

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
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);

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

		final List<ForFiscalPrincipal> principaux = fors.principaux;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipal fp = principaux.get(0);
		assertNotNull(fp);
		assertEquals(dateNaissance.addYears(18), fp.getDateDebut());
		assertNull(fp.getDateFin());
		assertEquals(MotifRattachement.DOMICILE, fp.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, fp.getModeImposition());
		assertEquals(MockCommune.Lausanne.getNoOFS(), fp.getNumeroOfsAutoriteFiscale().intValue());
	}

	/**
	 * Vérifie que le batch n'ouvre pas de for supplémentaire sur un habitant majeur qui en possède déjà.
	 * <p>
	 * Note: dans le cas réel de l'exécution du batch, le contribuable ci-dessous aurait été exclu du traitement au niveau de la requête
	 * SQL. Il est donc normal que le traitement provoque une erreur.
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
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(1, erreurs.size());

		final Erreur e = erreurs.get(0);
		assertNotNull(e);
		// le for existe déjà (voir remarque dans javadoc du test)
		assertEquals(OuvertureForsResults.ErreurType.INCOHERENCE_FOR_FISCAUX, e.raison);
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
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(1, erreurs.size());

		final Erreur e = erreurs.get(0);
		assertNotNull(e);
		assertEquals(OuvertureForsResults.ErreurType.DOMICILE_INCONNU, e.raison);
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
				addAdresse(individu2, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateNaissance, null);
				individu2.setNationalites(Collections.<Nationalite>emptyList());
			}
		});

		final PersonnePhysique h1 = addHabitant(noIndividu1);
		final PersonnePhysique h2 = addHabitant(noIndividu2);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.rapport = rapport;
		processor.traiteHabitant(h1.getNumero(), dateTraitement);
		processor.traiteHabitant(h2.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(2, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(2, erreurs.size());

		final Erreur e1 = erreurs.get(0);
		assertNotNull(e1);
		assertEquals(OuvertureForsResults.ErreurType.CIVIL_EXCEPTION, e1.raison);
		assertEquals("ch.vd.uniregctb.tiers.TiersException: Impossible de déterminer la nationalité de l'individu n°" + noIndividu1, e1.details);

		final Erreur e2 = erreurs.get(1);
		assertNotNull(e2);
		assertEquals(OuvertureForsResults.ErreurType.CIVIL_EXCEPTION, e2.raison);
		assertEquals("ch.vd.uniregctb.tiers.TiersException: Impossible de déterminer la nationalité de l'individu n°" + noIndividu2, e2.details);
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
		mockProcessor.rapport = rapport;
		mockProcessor.traiteHabitant(h1.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
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
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);

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
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.Gressy.BatimentLesPechauds, null, dateNaissance, null); // localité Gressy => commune de Gressy jusqu'à fin 2010
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.rapport = rapport;

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Gressy est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Gressy.getDateFinValidite().addMonths(-1));
		try {
			processor.traiteHabitant(h.getNumero(), dateTraitement);
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}

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

		final List<ForFiscalPrincipal> principaux = fors.principaux;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipal fp = principaux.get(0);
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
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.Gressy.BatimentLesPechauds, null, dateNaissance, null); // localité Gressy => commune d'Yverdon-les-Bains dès 2011
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement, tiersService, adresseService);
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

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

		final List<ForFiscalPrincipal> principaux = fors.principaux;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipal fp = principaux.get(0);
		assertForPrincipal(dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.YverdonLesBains, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fp);
	}
}
