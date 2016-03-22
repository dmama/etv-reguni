package ch.vd.uniregctb.evenement.organisation.engine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslatorImpl;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.uniregctb.evenement.organisation.interne.Indexation;
import ch.vd.uniregctb.evenement.organisation.interne.MessageSuivi;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.evenement.organisation.interne.information.InformationComplementaire;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.EN_ERREUR;
import static ch.vd.uniregctb.type.TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_FAILLITE;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class EvenementOrganisationProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	public EvenementOrganisationProcessorTest() {
		setWantIndexationTiers(true);
	}

	EvenementFiscalDAO evtFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	// TODO: Par analogie de EvenementCivilEchProcessorTest, contrôler les cas limites, comme la réindexation après la réception d'un message qui finit en erreur. etc...

	private static class SpyEvenementOrganisationTranslatorImpl extends EvenementOrganisationTranslatorImpl  {
		EvenementOrganisationInterne createdEvent;

		@Override
		public EvenementOrganisationInterne toInterne(EvenementOrganisation event, EvenementOrganisationOptions options) throws EvenementOrganisationException {
			createdEvent = super.toInterne(event, options);
			return createdEvent;
		}

		public EvenementOrganisationInterne getCreatedEvent() {
			return createdEvent;
		}
	}

	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testEvenementNeutreEntraineReindexation() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Lausanne));
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement
		final Long noEvenement = 12344321L;

		final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(2, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuivi);
		Assert.assertTrue(listEvtInterne.get(1) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	private List<EvenementOrganisationInterne> getListeEvtInternesCrees(SpyEvenementOrganisationTranslatorImpl translator) throws NoSuchFieldException,
			IllegalAccessException {
		final EvenementOrganisationInterne createdEvent = translator.getCreatedEvent();
		if (createdEvent instanceof EvenementOrganisationInterneComposite) {
			Class<?> spyClass = createdEvent.getClass();
			Field listEvtInterneField = spyClass.getDeclaredField("listEvtOrganisation");
			listEvtInterneField.setAccessible(true);
			return (List<EvenementOrganisationInterne>) listEvtInterneField.get(createdEvent);
		}
		return Collections.singletonList(createdEvent);
	}

	@Test(timeout = 10000L)
	public void testEvenementNeutreEntraineReindexationASecondTime() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Synergy SA", RegDate.get(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                 MockCommune.Lausanne)
				);
			}
		});

		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				return entreprise.getNumero();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Persistence événement
		final long noEvenement = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				// Création de l'événement
				final Long noEvenement = 12344321L;
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(2, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuivi);
		Assert.assertTrue(listEvtInterne.get(1) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = evtOrganisationDAO.get(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	@Test
	public void testClearAndAddOrderedErrors() throws Exception {
		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final EvenementOrganisation eventCreation = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), EN_ERREUR);
				EvenementOrganisation event = hibernateTemplate.merge(eventCreation);
				event.setErreurs(new ArrayList<EvenementOrganisationErreur>());
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("Erreur 1");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("Erreur 2");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("Erreur 3");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				EvenementOrganisation event = getUniqueEvent(noEvenement);
				Assert.assertEquals(3, event.getErreurs().size());

				event.getErreurs().clear();

				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("4");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("5");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("6");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("7");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("8");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("9");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("10");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("11");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("12");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("13");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("14");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("15");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("16");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("17");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("18");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("19");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
				{
					final EvenementOrganisationErreur e = new EvenementOrganisationErreur();
					e.setMessage("20");
					e.setType(TypeEvenementErreur.ERROR);
					event.getErreurs().add(e);
				}
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				EvenementOrganisation evenement = getUniqueEvent(noEvenement);
				Assert.assertEquals(17, evenement.getErreurs().size());
				StringBuilder orderSignature = new StringBuilder();
				for (EvenementOrganisationErreur err : evenement.getErreurs()) {
					orderSignature.append(err.getMessage());
				}
				Assert.assertEquals("4567891011121314151617181920", orderSignature.toString());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheeIdentifieeCorrectement() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE));

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nom + "Etab");
				addDomicileEtablissement(etablissement, dateDebut, null, MockCommune.Lausanne);

				return etablissement.getNumero();
			}
		});
		final long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, nom);
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				tiersService.addActiviteEconomique(etablissement, entreprise, dateDebut, true);

				return entreprise.getNumero();
			}
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(4, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuivi);
			String message = getMessageFromMessageSuivi((MessageSuivi) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Entreprise n°%d (%s) identifiée sur la base de ses attributs civils [%s].", noEntreprise, nom, nom), message);
		}
		{
			Assert.assertTrue(listEvtInterne.get(1) instanceof MessageSuivi);
			String message = getMessageFromMessageSuivi((MessageSuivi) listEvtInterne.get(1));
			Assert.assertEquals(String.format("Organisation civile n°%d rattachée avec succès à l'entreprise n°%d, avec tous ses établissements.", noOrganisation, noEntreprise), message);
		}
		Assert.assertTrue(listEvtInterne.get(2) instanceof InformationComplementaire);
		Assert.assertTrue(listEvtInterne.get(3) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheeIdentifieeCorrectementPartiel() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final String nom2 = "Synergy Renens SA";
		final String nom3 = "Synergy Aubonne SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;
		final Long noSite2 = noOrganisation + 1000001;

		final MockOrganisation organisation = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                                 TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                                 date(2010, 6, 24),
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2015, 7, 5), null, nom2, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, false,
		                                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nom + "Etab");
				addDomicileEtablissement(etablissement, dateDebut.getOneDayAfter(), null, MockCommune.Lausanne);

				return etablissement.getNumero();
			}
		});
		final long etablissement3Id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nom3 + "Etab");
				addDomicileEtablissement(etablissement, dateDebut.getOneDayAfter(), null, MockCommune.Aubonne);

				return etablissement.getNumero();
			}
		});
		final long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut.getOneDayAfter(), null, nom);
				addFormeJuridique(entreprise, dateDebut.getOneDayAfter(), null, FormeJuridiqueEntreprise.SARL);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);
				tiersService.addActiviteEconomique(etablissement, entreprise, dateDebut.getOneDayAfter(), true);

				final Etablissement etablissement3 = (Etablissement) tiersDAO.get(etablissement3Id);
				tiersService.addActiviteEconomique(etablissement3, entreprise, dateDebut.getOneDayAfter(), false);

				addRegimeFiscalVD(entreprise, dateDebut.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut.getOneDayAfter(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
				return entreprise.getNumero();
			}
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(5, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuivi);
			String message = getMessageFromMessageSuivi((MessageSuivi) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Entreprise n°%d (%s) identifiée sur la base de ses attributs civils [%s].", noEntreprise, nom, nom), message);
		}
		{
			Assert.assertTrue(listEvtInterne.get(1) instanceof MessageSuivi);
			String message = getMessageFromMessageSuivi((MessageSuivi) listEvtInterne.get(1));
			Assert.assertEquals(
					String.format("Organisation civile n°%d rattachée à l'entreprise n°%d. Cependant, certains établissements n'ont pas trouvé d'équivalent civil: n°%d. Aussi des sites civils secondaires n'ont pas pu être rattachés et seront créés: n°%d",
					              noOrganisation, noEntreprise, etablissement3Id, noSite2), message);
		}
		Assert.assertTrue(listEvtInterne.get(3) instanceof InformationComplementaire);
		Assert.assertTrue(listEvtInterne.get(4) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	private String getMessageFromMessageSuivi(MessageSuivi msgSuivi) throws NoSuchFieldException, IllegalAccessException {
		Class<?> spyClass = msgSuivi.getClass();
		Field suiviField = spyClass.getDeclaredField("suivi");
		suiviField.setAccessible(true);
		return (String) suiviField.get(msgSuivi);
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheePlusieursPossibles() throws Exception {

		// Mise en place service mock
		final RegDate dateDeDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateDeDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE));

			}
		});

		// Création des entreprises

		long noEntrerpise1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDeDebut, null, "Synergy truc bidule");
				return entreprise.getNumero();
			}
		});

		long noEntrerpise2 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDeDebut, null, "Synergy machin chose");
				return entreprise.getNumero();
			}
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(1, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof TraitementManuel);
		String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(0));
		Assert.assertEquals(String.format("Plusieurs entreprises ont été trouvées (numéros [%d, %d]) pour les attributs civils [%s]. Arrêt du traitement.",
		                                  noEntrerpise1, noEntrerpise2, nom),
		                    message);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testAucuneEntrepriseIdentifiee() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE));

			}
		});

		// Création des entreprises

		long noEntrerpise1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, "Syntétiques Sarl");
				return entreprise.getNumero();
			}
		});

		long noEntrerpise2 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, "Synergios S.A.");
				return entreprise.getNumero();
			}
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2105, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(3, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuivi);
		String message = getMessageFromMessageSuivi((MessageSuivi) listEvtInterne.get(0));
		Assert.assertEquals(String.format("Aucune entreprise identifiée pour le numéro civil %s ou les attributs civils [%s].",
		                                  noOrganisation, nom),
		                    message);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	private String getMessageFromTraitementManuel(TraitementManuel msg) throws NoSuchFieldException, IllegalAccessException {
		Class<?> spyClass = msg.getClass();
		Field messageField = spyClass.getDeclaredField("message");
		messageField.setAccessible(true);
		return (String) messageField.get(msg);
	}

	@Test(timeout = 1000000L)
	public void testEntrepriseConnueMaisNouvelleAuCivil() throws Exception {

		// Mise en place service mock
		final RegDate dateEvt = date(2015, 6, 26);
		final String nom = "Synergy SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateEvt, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE));

			}
		});

		// Création de l'entreprise

		long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				addRaisonSocialeFiscaleEntreprise(entreprise, dateEvt, null, nom);
				return entreprise.getNumero();
			}
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, dateEvt, A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertTrue(listEvtInterne.size() > 2);
		Assert.assertTrue(listEvtInterne.get(2) instanceof TraitementManuel);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testPlusieursEntreprisesRapprochees() throws Exception {

		// Mise en place service mock
		final RegDate dateDeDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateDeDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE));

			}
		});

		// Création des entreprises

		long noEntrerpise1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDeDebut, null, "Synergy truc bidule");
				return entreprise.getNumero();
			}
		});

		long noEntrerpise2 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				addRaisonSocialeFiscaleEntreprise(entreprise, dateDeDebut, null, "Synergy machin chose");
				return entreprise.getNumero();
			}
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Mise en place Translator "espion"
		SpyEvenementOrganisationTranslatorImpl translator = new SpyEvenementOrganisationTranslatorImpl();

		translator.setServiceOrganisationService(serviceOrganisation);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		List<EvenementOrganisationInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(1, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof TraitementManuel);
		String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(0));
		Assert.assertEquals(String.format("Plusieurs entreprises non-annulées partagent le même numéro d'organisation 101202100: ([%d, %d])",
		                                  noEntrerpise1, noEntrerpise2),
		                    message);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
	}
}
