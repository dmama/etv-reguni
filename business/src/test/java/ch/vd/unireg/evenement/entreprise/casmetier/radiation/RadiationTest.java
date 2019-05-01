package ch.vd.unireg.evenement.entreprise.casmetier.radiation;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.entreprise.engine.translator.EvenementEntrepriseTranslatorImpl;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
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
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL;
import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class RadiationTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public RadiationTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;
	private EvenementEntrepriseTranslatorImpl translator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");

		// Configuration par défaut du translator
		translator = new EvenementEntrepriseTranslatorImpl();
		translator.setServiceEntreprise(serviceEntreprise);
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
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setAudit(audit);
		translator.afterPropertiesSet();
	}

	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testRadiationEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		final Entreprise entreprise = doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.singletonList(new VaudoisOrdinaire(entreprise, date(2010, 6, 24), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, AssujettissementPersonnesMoralesCalculator.COMMUNE_ANALYZER));
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise
		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissementSecondaire = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(noEntrepriseCivile, noEtablissement, noEtablissementSecondaire, "Synergy SA", date(2010, 6, 26), null,
						                                                                  FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.LaSarraz.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                                                  StatusInscriptionRC.ACTIF, dateInscription,
						                                                                  StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, TypeEntrepriseRegistreIDE.SITE,
						                                                                  "CHE999999996", "CHE199999996");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRC rc2 = (MockDonneesRC) entreprise.getEtablissements().get(1).getDonneesRC();
				rc2.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                          dateInscription, dateRadiation,
				                                                          dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				final MockDonneesRegistreIDE donneesRegistreIDE2 = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(1).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		final Long tiersNo = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.LaSarraz, MotifRattachement.ETABLISSEMENT_STABLE);

			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		doInNewTransactionAndSession(transactionStatus -> (Entreprise) tiersDAO.get(tiersNo));

		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> addEntrepriseConnueAuCivil(noEntrepriseCivile).getNumero());

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association sympa", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise12 = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise12, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise12;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.singletonList(new VaudoisOrdinaire(entreprise, date(2010, 6, 24), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, AssujettissementPersonnesMoralesCalculator.COMMUNE_ANALYZER));
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(TypeEtatEntreprise.FONDEE, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Mutation : Radiation APM",
			             evt.getErreurs().get(1).getMessage());
			assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.RADIEE_RC.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(entreprise1.getNumero()), noEntrepriseCivile),
			             evt.getErreurs().get(2).getMessage());
			assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.FONDEE.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(entreprise1.getNumero()), noEntrepriseCivile),
			             evt.getErreurs().get(3).getMessage());
			assertEquals("Vérification requise pour la radiation de l'association / fondation encore assujettie sortie du RC.",
			             evt.getErreurs().get(4).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationAPMRCNonAssujettie() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association sympa", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise12 = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise12, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise12;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.emptyList();
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Mutation : Radiation APM",
			             evt.getErreurs().get(1).getMessage());
			assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.RADIEE_RC.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(entreprise1.getNumero()), noEntrepriseCivile),
			             evt.getErreurs().get(2).getMessage());
			assertEquals("Vérification requise pour l'association / fondation radiée du RC.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationAPMRCEnFaillitemaisResteVivante() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association sympa", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", null, null);
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise12 = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.INSCRITE_RC, entreprise12, date(2010, 6, 24), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			tiersService.changeEtatEntreprise(TypeEtatEntreprise.EN_FAILLITE, entreprise12, date(2012, 1, 1), TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise12;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Configuration du translator
		translator.setAssujettissementService(new MockAssujettissementService() {
			@Nullable
			@Override
			public List<Assujettissement> determine(Contribuable ctb) {
				return Collections.singletonList(new VaudoisOrdinaire(entreprise, date(2010, 6, 24), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, AssujettissementPersonnesMoralesCalculator.COMMUNE_ANALYZER));
			}
		});
		translator.afterPropertiesSet();

		buildProcessor(translator);


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(TypeEtatEntreprise.EN_FAILLITE, entreprise1.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("Mutation : Radiation APM",
			             evt.getErreurs().get(1).getMessage());
			assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.RADIEE_RC.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(entreprise1.getNumero()), noEntrepriseCivile),
			             evt.getErreurs().get(2).getMessage());
			assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.EN_FAILLITE.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(entreprise1.getNumero()), noEntrepriseCivile),
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
		final Long noEntrepriseCivile = 102059155L;
		final Long noEtablissement = 102059156L;

		final RegDate dateCreation = date(2018, 1, 11);
		final RegDate dateRadiation = date(2018, 1, 29);

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {

				// source : http://rp-ws-va.etat-de-vaud.ch/registres/rcent/services/v3/organisation/CT.VD.PARTY/102059155?history=true
				final RegDate dateSnapshot1 = RegDate.get(2018, 1, 16);
				final RegDate dateSnapshot2 = RegDate.get(2018, 2, 1);

				final MockDonneesRC donneesRC = new MockDonneesRC();
				donneesRC.addInscription(dateSnapshot1, dateSnapshot2.getOneDayBefore(), new InscriptionRC(StatusInscriptionRC.ACTIF, null, dateCreation, null, dateCreation, null));
				donneesRC.addInscription(dateSnapshot2, null, new InscriptionRC(StatusInscriptionRC.RADIE, null, dateCreation, dateRadiation, dateCreation, dateRadiation));

				final MockEtablissementCivil etablissement = new MockEtablissementCivil(noEtablissement, new MockDonneesRegistreIDE(), donneesRC, new MockDonneesREE());
				etablissement.changeNom(dateSnapshot1, "By Hina Boutique SNC");
				etablissement.changeTypeEtablissement(dateSnapshot1, ETABLISSEMENT_PRINCIPAL);
				etablissement.changeFormeLegale(dateSnapshot1, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF);
				etablissement.changeDomicile(dateSnapshot1, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vallorbe.getNoOFS());
				etablissement.changeDomicile(dateSnapshot2, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vallorbe.getNoOFS());

				final MockEntrepriseCivile entreprise = new MockEntrepriseCivile(noEntrepriseCivile);
				entreprise.addDonneesEtablissement(etablissement);
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise
		final Long entrepriseId = doInNewTransactionAndSession(transactionStatus -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_RADIATION_ENTREPRISE, RegDate.get(2018, 2, 1), A_TRAITER);
			return hibernateTemplate.merge(event);
		});

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			// la SNC doit maintenant être radiée
			final Entreprise entreprise1 = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise1.getEtatActuel().getType());

			// un message de vérification doit être présent
			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			assertEquals(4, erreurs.size());
			assertEquals("Entreprise n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseId) + " (By Hina Boutique SNC) identifiée sur la base du numéro civil 102059155 (numéro cantonal).", erreurs.get(0).getMessage());
			assertEquals("Mutation : Radiation", erreurs.get(1).getMessage());
			assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.RADIEE_RC.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(entreprise1.getNumero()), noEntrepriseCivile),
			             erreurs.get(2).getMessage());
			assertEquals("Vérification requise pour la radiation de l'entreprise encore dotée d'un for principal.", erreurs.get(3).getMessage());
			return null;
		});
	}
}
