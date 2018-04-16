package ch.vd.unireg.evenement.organisation.casmetier.radiation;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationService;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.evenement.organisation.engine.translator.EvenementOrganisationTranslatorImpl;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementPersonnesMoralesCalculator;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.MockAssujettissementService;
import ch.vd.unireg.metier.assujettissement.MotifAssujettissement;
import ch.vd.unireg.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementOrganisation;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_PRINCIPAL;
import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class RadiationTest extends AbstractEvenementOrganisationProcessorTest {

	public RadiationTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;
	private EvenementOrganisationTranslatorImpl translator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");

		// Configuration par défaut du translator
		translator = new EvenementOrganisationTranslatorImpl();
		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementOrganisationService(getBean(EvenementOrganisationService.class, "evtOrganisationService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();
	}

	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testRadiationOrganisation() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("L'entreprise a été radiée du registre du commerce.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationAvecAssujetissement() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		final Entreprise entreprise = doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.singletonList(new VaudoisOrdinaire(entreprise, date(2010, 6, 24), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, AssujettissementPersonnesMoralesCalculator.COMMUNE_ANALYZER));
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Vérification requise pour la radiation de l'entreprise encore assujettie.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationSansAssujAvecForPrincipal() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);

			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Vérification requise pour la radiation de l'entreprise encore dotée d'un for principal.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationSansAssujAvecForPrincipalDansLeFutur() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise
		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			addForPrincipal(entreprise, date(2015, 10, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);

			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Vérification requise pour la radiation de l'entreprise encore dotée d'un for principal.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationSansAssujAvecForSecondaire() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;
		final Long noSiteSecondaire = noOrganisation + 2000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisationAvecSiteSecondaire(noOrganisation, noSite, noSiteSecondaire, "Synergy SA", date(2010, 6, 26), null,
						                                                             FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                             TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.LaSarraz.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                                             StatusInscriptionRC.ACTIF, dateInscription,
						                                                             StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, TypeOrganisationRegistreIDE.SITE,
						                                                             "CHE999999996", "CHE199999996");
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRC rc2 = (MockDonneesRC) organisation.getDonneesSites().get(1).getDonneesRC();
				rc2.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                          dateInscription, dateRadiation,
				                                                          dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				final MockDonneesRegistreIDE donneesRegistreIDE2 = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(1).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.LaSarraz.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE);

			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Vérification requise pour la radiation de l'entreprise encore dotée d'un for principal ainsi que d'un ou plusieurs for secondaires.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationIDEPasRC() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> addEntrepriseConnueAuCivil(noOrganisation).getNumero());

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

			// vérification des événements fiscaux
/*
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals(String.format("L'entreprise Entreprise n°%d est radiée de l'IDE mais pas du RC!", tiersId),
			                    evt.getErreurs().get(1).getMessage());
*/
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationAPMRCmaisResteVivante() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Association sympa", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise12 = addEntrepriseConnueAuCivil(noOrganisation);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise12, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise12;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.singletonList(new VaudoisOrdinaire(entreprise, date(2010, 6, 24), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, AssujettissementPersonnesMoralesCalculator.COMMUNE_ANALYZER));
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.FONDEE, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Mutation : Radiation APM",
			             evt.getErreurs().get(1).getMessage());
			assertEquals("Réglage de l'état: Radiée du RC.",
			             evt.getErreurs().get(2).getMessage());
			assertEquals("Réglage de l'état: Fondée.",
			             evt.getErreurs().get(3).getMessage());
			assertEquals("Vérification requise pour la radiation de l'association / fondation encore assujettie sortie du RC.",
			             evt.getErreurs().get(4).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationAPMRCNonAssujettie() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Association sympa", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise12 = addEntrepriseConnueAuCivil(noOrganisation);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise12, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise12;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Mutation : Radiation APM",
			             evt.getErreurs().get(1).getMessage());
			assertEquals("Réglage de l'état: Radiée du RC.",
			             evt.getErreurs().get(2).getMessage());
			assertEquals("Vérification requise pour l'association / fondation radiée du RC.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationAPMRCEnFaillitemaisResteVivante() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Association sympa", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise12 = addEntrepriseConnueAuCivil(noOrganisation);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise12, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.EN_FAILLITE, entreprise12, date(2012, 1, 1), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise12;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.singletonList(new VaudoisOrdinaire(entreprise, date(2010, 6, 24), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, AssujettissementPersonnesMoralesCalculator.COMMUNE_ANALYZER));
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.EN_FAILLITE, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Mutation : Radiation APM",
			             evt.getErreurs().get(1).getMessage());
			assertEquals("Réglage de l'état: Radiée du RC.",
			             evt.getErreurs().get(2).getMessage());
			assertEquals("Réglage de l'état: En faillite.",
			             evt.getErreurs().get(3).getMessage());
			assertEquals("On considère que l'association / fondation reste en activité puisqu'elle est toujours assujettie, bien qu'elle soit en faillite.",
			             evt.getErreurs().get(4).getMessage());
			assertEquals("Vérification requise pour la radiation de l'association / fondation encore assujettie sortie du RC, qui reste en faillite.",
			             evt.getErreurs().get(5).getMessage());
			return null;
		});
	}

	/**
	 * [SIFISC-19494] Ce test vérifie que la radiation d'une SNC avec un for vaudois (ou un assujettissement) fait bien passer l'événement dans l'état A_VERIFIER.
	 */
	@Test(timeout = 10000L)
	public void testRadiationSNC() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 102059155L;
		final Long noSite = 102059156L;

		final RegDate dateCreation = date(2018, 1, 11);
		final RegDate dateRadiation = date(2018, 1, 29);

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				// source : http://rp-ws-va.etat-de-vaud.ch/registres/rcent/services/v3/organisation/CT.VD.PARTY/102059155?history=true
				final RegDate dateSnapshot1 = RegDate.get(2018, 1, 16);
				final RegDate dateSnapshot2 = RegDate.get(2018, 2, 1);

				final MockDonneesRC donneesRC = new MockDonneesRC();
				donneesRC.addInscription(dateSnapshot1, dateSnapshot2.getOneDayBefore(), new InscriptionRC(StatusInscriptionRC.ACTIF, null, dateCreation, null, dateCreation, null));
				donneesRC.addInscription(dateSnapshot2, null, new InscriptionRC(StatusInscriptionRC.RADIE, null, dateCreation, dateRadiation, dateCreation, dateRadiation));

				final MockSiteOrganisation site = new MockSiteOrganisation(noSite, new MockDonneesRegistreIDE(), donneesRC, new MockDonneesREE());
				site.changeNom(dateSnapshot1, "By Hina Boutique SNC");
				site.changeTypeDeSite(dateSnapshot1, ETABLISSEMENT_PRINCIPAL);
				site.changeFormeLegale(dateSnapshot1, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF);
				site.changeDomicile(dateSnapshot1, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vallorbe.getNoOFS());
				site.changeDomicile(dateSnapshot2, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vallorbe.getNoOFS());

				final MockOrganisation organisation = new MockOrganisation(noOrganisation);
				organisation.addDonneesSite(site);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise
		final Long entrepriseId = doInNewTransactionAndSession(transactionStatus -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise, dateCreation, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addRegimeFiscalVD(entreprise, RegDate.get(2018, 1, 12), null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, RegDate.get(2018, 1, 12), null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addForPrincipal(entreprise, RegDate.get(2018, 1, 12), MotifFor.DEBUT_EXPLOITATION, MockCommune.Vallorbe, GenreImpot.REVENU_FORTUNE);
			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 1189567L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_RADIATION_ENTREPRISE, RegDate.get(2018, 2, 1), A_TRAITER);
			return hibernateTemplate.merge(event);
		});

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementOrganisation evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

			// la SNC doit maintenant être radiée
			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// un message de vérification doit être présent
			final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
			assertEquals(4, erreurs.size());
			assertEquals("Entreprise n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseId) + " (By Hina Boutique SNC) identifiée sur la base du numéro civil 102059155 (numéro cantonal).", erreurs.get(0).getMessage());
			assertEquals("Mutation : Radiation", erreurs.get(1).getMessage());
			assertEquals("Réglage de l'état: Radiée du RC.", erreurs.get(2).getMessage());
			assertEquals("Vérification requise pour la radiation de l'entreprise encore dotée d'un for principal.", erreurs.get(3).getMessage());
			return null;
		});
	}
}
