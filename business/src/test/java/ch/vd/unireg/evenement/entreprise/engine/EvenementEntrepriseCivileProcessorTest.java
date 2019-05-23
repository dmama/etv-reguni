package ch.vd.unireg.evenement.entreprise.engine;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseCappingLevelProvider;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.engine.translator.EvenementEntrepriseTranslatorImpl;
import ch.vd.unireg.evenement.entreprise.engine.translator.NiveauCappingEtat;
import ch.vd.unireg.evenement.entreprise.interne.CappingAVerifier;
import ch.vd.unireg.evenement.entreprise.interne.CappingEnErreur;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneComposite;
import ch.vd.unireg.evenement.entreprise.interne.Indexation;
import ch.vd.unireg.evenement.entreprise.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.evenement.entreprise.interne.ValideurDebutDeTraitement;
import ch.vd.unireg.evenement.entreprise.interne.information.InformationComplementaire;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementErreur;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static ch.vd.unireg.type.EtatEvenementEntreprise.EN_ERREUR;
import static ch.vd.unireg.type.EtatEvenementEntreprise.TRAITE;
import static ch.vd.unireg.type.TypeEvenementEntreprise.FOSC_AUTRE_MUTATION;
import static ch.vd.unireg.type.TypeEvenementEntreprise.FOSC_COMMUNICATION_DANS_FAILLITE;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class EvenementEntrepriseCivileProcessorTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public EvenementEntrepriseCivileProcessorTest() {
		setWantIndexationTiers(true);
	}

	EvenementFiscalDAO evtFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	// TODO: Par analogie de EvenementCivilEchProcessorTest, contrôler les cas limites, comme la réindexation après la réception d'un message qui finit en erreur. etc...

	private static class SpyEvenementEntrepriseTranslatorImpl extends EvenementEntrepriseTranslatorImpl {
		EvenementEntrepriseInterne createdEvent;

		@Override
		public EvenementEntrepriseInterne toInterne(EvenementEntreprise event) throws EvenementEntrepriseException {
			createdEvent = super.toInterne(event);
			return createdEvent;
		}

		public EvenementEntrepriseInterne getCreatedEvent() {
			return createdEvent;
		}
	}

	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testEvenementNeutreEntraineReindexation() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Lausanne));
			}
		});

		doInNewTransactionAndSession(status -> addEntrepriseConnueAuCivil(noEntrepriseCivile));

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement
		final Long noEvenement = 12344321L;

		final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);

		// Persistence événement
		doInNewTransactionAndSession(status -> hibernateTemplate.merge(event).getId());

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(3, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
		Assert.assertTrue(listEvtInterne.get(2) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementNeutreAvecCappingAVerifier() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Lausanne));
			}
		});

		final long idEntreprise = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			return entreprise.getNumero();
		});

		// Mise en place Translator "espion"
		final SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setCappingLevelProvider(new EvenementEntrepriseCappingLevelProvider() {
			@Override
			public NiveauCappingEtat getNiveauCapping() {
				return NiveauCappingEtat.A_VERIFIER;
			}
		});
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement
		final Long noEvenement = 12344321L;

		final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);

		// Persistence événement
		doInNewTransactionAndSession(status -> hibernateTemplate.merge(event).getId());

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		final List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(4, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
		Assert.assertTrue(listEvtInterne.get(2) instanceof Indexation);
		Assert.assertTrue(listEvtInterne.get(3) instanceof CappingAVerifier);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final List<EvenementEntrepriseErreur> messages = evt.getErreurs();
			Assert.assertNotNull(messages);
			Assert.assertEquals(2, messages.size());
			Assert.assertEquals(String.format("Entreprise n°%s (Synergy SA, IDE: CHE-999.999.996) identifiée sur la base du numéro civil %d (numéro cantonal).", FormatNumeroHelper.numeroCTBToDisplay(idEntreprise), noEntrepriseCivile),
			                    messages.get(0).getMessage());
			Assert.assertEquals("Evénement explicitement placé 'à vérifier' par configuration applicative.", messages.get(1).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementNeutreAvecCappingEnErreur() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Lausanne));
			}
		});

		final long idEntreprise = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			return entreprise.getNumero();
		});

		// Mise en place Translator "espion"
		final SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setCappingLevelProvider(new EvenementEntrepriseCappingLevelProvider() {
			@Override
			public NiveauCappingEtat getNiveauCapping() {
				return NiveauCappingEtat.EN_ERREUR;
			}
		});
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement
		final Long noEvenement = 12344321L;

		final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);

		// Persistence événement
		doInNewTransactionAndSession(status -> hibernateTemplate.merge(event).getId());

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		final List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(4, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
		Assert.assertTrue(listEvtInterne.get(2) instanceof Indexation);
		Assert.assertTrue(listEvtInterne.get(3) instanceof CappingEnErreur);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final List<EvenementEntrepriseErreur> messages = evt.getErreurs();
			Assert.assertNotNull(messages);
			Assert.assertEquals(2, messages.size());
			Assert.assertEquals(String.format("Entreprise n°%s (Synergy SA, IDE: CHE-999.999.996) identifiée sur la base du numéro civil %d (numéro cantonal).", FormatNumeroHelper.numeroCTBToDisplay(idEntreprise), noEntrepriseCivile),
			                    messages.get(0).getMessage());
			Assert.assertEquals("Evénement explicitement placé 'en erreur' par configuration applicative. Toutes les modifications apportées pendant le traitement sont abandonnées.", messages.get(1).getMessage());
			return null;
		});
	}

	private List<EvenementEntrepriseInterne> getListeEvtInternesCrees(SpyEvenementEntrepriseTranslatorImpl translator) throws NoSuchFieldException,
			IllegalAccessException {
		final EvenementEntrepriseInterne createdEvent = translator.getCreatedEvent();
		if (createdEvent instanceof EvenementEntrepriseInterneComposite) {
			Class<?> spyClass = createdEvent.getClass();
			Field listEvtInterneField = spyClass.getDeclaredField("listEvtEntreprise");
			listEvtInterneField.setAccessible(true);
			return (List<EvenementEntrepriseInterne>) listEvtInterneField.get(createdEvent);
		}
		return Collections.singletonList(createdEvent);
	}

	@Test(timeout = 10000L)
	public void testEvenementNeutreEntraineReindexationASecondTime() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne)
				);
			}
		});

		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			return entreprise.getNumero();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Persistence événement
		final long noEvenement = doInNewTransactionAndSession(status -> {
			// Création de l'événement
			final Long no = 12344321L;
			final EvenementEntreprise event = createEvent(no, noEntrepriseCivile, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(3, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
		Assert.assertTrue(listEvtInterne.get(2) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = evtEntrepriseDAO.get(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test
	public void testClearAndAddOrderedErrors() throws Exception {
		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise eventCreation = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), EN_ERREUR);
			EvenementEntreprise event = hibernateTemplate.merge(eventCreation);
			event.setErreurs(new ArrayList<>());
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("Erreur 1");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("Erreur 2");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("Erreur 3");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			;
			return null;
		});

		doInNewTransactionAndSession(status -> {
			EvenementEntreprise event = getUniqueEvent(noEvenement);
			Assert.assertEquals(3, event.getErreurs().size());

			event.getErreurs().clear();

			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("4");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("5");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("6");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("7");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("8");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("9");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("10");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("11");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("12");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("13");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("14");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("15");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("16");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("17");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("18");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("19");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			{
				final EvenementEntrepriseErreur e = new EvenementEntrepriseErreur();
				e.setMessage("20");
				e.setType(TypeEvenementErreur.ERROR);
				event.getErreurs().add(e);
			}
			return null;
		});

		doInNewTransactionAndSession(status -> {
			EvenementEntreprise evenement = getUniqueEvent(noEvenement);
			Assert.assertEquals(17, evenement.getErreurs().size());
			StringBuilder orderSignature = new StringBuilder();
			for (EvenementEntrepriseErreur err : evenement.getErreurs()) {
				orderSignature.append(err.getMessage());
			}
			Assert.assertEquals("4567891011121314151617181920", orderSignature.toString());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheeIdentifieeCorrectement() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom + "Etab");
			addDomicileEtablissement(etablissement, dateDebut, null, MockCommune.Lausanne);
			return etablissement.getNumero();
		});
		final long noEntreprise = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, nom);
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			tiersService.addActiviteEconomique(etablissement, entreprise, dateDebut, true);
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(5, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Entreprise n°%s (%s) identifiée sur la base de ses attributs civils [%s, IDE: CHE-999.999.996].", FormatNumeroHelper.numeroCTBToDisplay(noEntreprise), nom, nom), message);
		}
		{
			Assert.assertTrue(listEvtInterne.get(1) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(1));
			Assert.assertEquals(String.format("Entreprise civile n°%d rattachée avec succès à l'entreprise n°%s, avec tous ses établissements.", noEntrepriseCivile, FormatNumeroHelper.numeroCTBToDisplay(noEntreprise)), message);
		}
		Assert.assertTrue(listEvtInterne.get(3) instanceof InformationComplementaire);
		Assert.assertTrue(listEvtInterne.get(4) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheeIdentifieeCorrectementPartiel() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final RegDate dateRC = date(2010, 6, 24);
		final String nom = "Synergy SA";
		final String nom2 = "Synergy Renens SA";
		final String nom3 = "Synergy Aubonne SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 1000001;

		final MockEntrepriseCivile entreprise = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                               dateRC,
		                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockEtablissementCivilFactory.addEtablissement(noEtablissement2, entreprise, date(2015, 7, 5), null, nom2, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), StatusInscriptionRC.ACTIF, dateRC,
		                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom + "Etab");
			addDomicileEtablissement(etablissement, dateRC.getOneDayAfter(), null, MockCommune.Lausanne);
			return etablissement.getNumero();
		});
		final long etablissement3Id = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom3 + "Etab");
			addDomicileEtablissement(etablissement, dateRC.getOneDayAfter(), null, MockCommune.Aubonne);
			return etablissement.getNumero();
		});
		final long noEntreprise = doInNewTransactionAndSession(status -> {
			final Entreprise ent = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(ent, dateRC.getOneDayAfter(), null, nom);
			addFormeJuridique(ent, dateRC.getOneDayAfter(), null, FormeJuridiqueEntreprise.SARL);

			final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);
			tiersService.addActiviteEconomique(etablissement, ent, dateRC.getOneDayAfter(), true);

			final Etablissement etablissement3 = (Etablissement) tiersDAO.get(etablissement3Id);
			tiersService.addActiviteEconomique(etablissement3, ent, dateRC.getOneDayAfter(), false);

			addRegimeFiscalVD(ent, dateRC.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(ent, dateRC.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(ent, dateRC.getOneDayAfter(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return ent.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(6, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Entreprise n°%s (%s) identifiée sur la base de ses attributs civils [%s, IDE: CHE-999.999.996].", FormatNumeroHelper.numeroCTBToDisplay(noEntreprise), nom, nom), message);
		}
		{
			Assert.assertTrue(listEvtInterne.get(1) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(1));
			Assert.assertEquals(
					String.format(
							"Entreprise civile n°%d rattachée à l'entreprise n°%s. Cependant, certains établissements n'ont pas trouvé d'équivalent civil: n°%s. Aussi des établissements civils secondaires n'ont pas pu être rattachés et seront éventuellement créés: n°%d",
							noEntrepriseCivile, FormatNumeroHelper.numeroCTBToDisplay(noEntreprise), FormatNumeroHelper.numeroCTBToDisplay(etablissement3Id), noEtablissement2), message);
		}
		Assert.assertTrue(listEvtInterne.get(4) instanceof InformationComplementaire);
		Assert.assertTrue(listEvtInterne.get(5) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			Assert.assertEquals(
					"Refus de créer dans Unireg l'établissement n°102202101 (Synergy Renens SA, à Renens VD, IDE: CHE-999.999.997) dont la fondation / déménagement remonte à 24.06.2010, 1837 jours avant la date de l'événement. La tolérance étant de 15 jours. Il y a probablement une erreur d'identification ou un problème de date.",
					evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	private String getMessageFromMessageSuiviPreExecution(MessageSuiviPreExecution msgSuivi) throws NoSuchFieldException, IllegalAccessException {
		Class<?> spyClass = msgSuivi.getClass();
		Field suiviField = spyClass.getDeclaredField("suivi");
		suiviField.setAccessible(true);
		return (String) suiviField.get(msgSuivi);
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheeIdentifieeCorrectementAvecESPartiel() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final RegDate dateRC = date(2010, 6, 24);
		final String nom = "Synergy SA";
		final String nom2 = "Synergy Renens SA";
		final String nom3 = "Synergy Aubonne SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 1000001;

		final MockEntrepriseCivile entreprise = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                               dateRC,
		                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockEtablissementCivilFactory.addEtablissement(noEtablissement2, entreprise, date(2015, 7, 5), null, nom2, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), StatusInscriptionRC.ACTIF, dateRC,
		                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom + "Etab");
			addDomicileEtablissement(etablissement, dateRC.getOneDayAfter(), null, MockCommune.Lausanne);
			return etablissement.getNumero();
		});
		final long etablissement2Id = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom2 + "Etab");
			addDomicileEtablissement(etablissement, dateRC.getOneDayAfter(), null, MockCommune.Renens);
			return etablissement.getNumero();
		});
		final long etablissement3Id = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom3 + "Etab");
			addDomicileEtablissement(etablissement, dateRC.getOneDayAfter(), null, MockCommune.Aubonne);
			return etablissement.getNumero();
		});
		final long noEntreprise = doInNewTransactionAndSession(status -> {
			final Entreprise ent = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(ent, dateRC.getOneDayAfter(), null, nom);
			addFormeJuridique(ent, dateRC.getOneDayAfter(), null, FormeJuridiqueEntreprise.SARL);

			final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);
			tiersService.addActiviteEconomique(etablissement, ent, dateRC.getOneDayAfter(), true);

			final Etablissement etablissement2 = (Etablissement) tiersDAO.get(etablissement2Id);
			tiersService.addActiviteEconomique(etablissement2, ent, dateRC.getOneDayAfter(), false);

			final Etablissement etablissement3 = (Etablissement) tiersDAO.get(etablissement3Id);
			tiersService.addActiviteEconomique(etablissement3, ent, dateRC.getOneDayAfter(), false);

			addRegimeFiscalVD(ent, dateRC.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(ent, dateRC.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(ent, dateRC.getOneDayAfter(), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return ent.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(6, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Entreprise n°%s (%s) identifiée sur la base de ses attributs civils [%s, IDE: CHE-999.999.996].", FormatNumeroHelper.numeroCTBToDisplay(noEntreprise), nom, nom), message);
		}
		{
			Assert.assertTrue(listEvtInterne.get(1) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(1));
			Assert.assertEquals(
					String.format("Entreprise civile n°%d rattachée à l'entreprise n°%s. Cependant, certains établissements n'ont pas trouvé d'équivalent civil: n°%s.",
					              noEntrepriseCivile, FormatNumeroHelper.numeroCTBToDisplay(noEntreprise), FormatNumeroHelper.numeroCTBToDisplay(etablissement3Id)), message);
		}
		Assert.assertTrue(listEvtInterne.get(4) instanceof InformationComplementaire);
		Assert.assertTrue(listEvtInterne.get(5) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheeIdentifieCorrectementDoublonRCEnt() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEntrepriseCivileDoublon = 201202100L;
		final Long noEtablissementDoublon = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivileDoublon, noEtablissementDoublon, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom + "Etab");
			etablissement.setNumeroEtablissement(noEtablissement);
			addDomicileEtablissement(etablissement, dateDebut, null, MockCommune.Lausanne);
			return etablissement.getNumero();
		});
		final long noEntreprise = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, nom);
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			tiersService.addActiviteEconomique(etablissement, entreprise, dateDebut, true);
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivileDoublon, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivileDoublon);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(1, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof TraitementManuel);
			String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(0));
			Assert.assertEquals(String.format(
					"Entreprise n°%s identifiée sur la base de ses attributs civils [%s, IDE: CHE-999.999.996], mais déjà rattachée à l'entreprise civile n°101202100 (%s, IDE: CHE-999.999.996). Potentiel doublon au civil. Traitement manuel.",
					FormatNumeroHelper.numeroCTBToDisplay(noEntreprise), nom, nom), message);
		}

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEntrepriseNonRapprocheePlusieursPossibles() throws Exception {

		// Mise en place service mock
		final RegDate dateDeDebut = date(2010, 6, 26);
		final RegDate dateRC = date(2010, 6, 24);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDeDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
				                                                     StatusInscriptionRC.ACTIF, dateRC,
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création des entreprises

		long noEntrerpise1 = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateRC, null, "Synergy truc bidule");
			return entreprise.getNumero();
		});

		long noEntrerpise2 = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateRC, null, "Synergy machin chose");
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(1, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof TraitementManuel);
		String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(0));
		Assert.assertEquals(String.format("Plusieurs entreprises ont été trouvées (numéros [%d, %d]) pour les attributs civils [%s, IDE: CHE-999.999.996]. Arrêt du traitement.",
		                                  noEntrerpise1, noEntrerpise2, nom),
		                    message);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testAucuneEntrepriseIdentifiee() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création des entreprises

		long noEntrerpise1 = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, "Syntétiques Sarl");
			return entreprise.getNumero();
		});

		long noEntrerpise2 = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, "Synergios S.A.");
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2105, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(4, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
		String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(0));
		Assert.assertEquals(String.format("Aucune entreprise identifiée pour le numéro civil %s ou les attributs civils [%s, IDE: CHE-999.999.996].",
		                                  noEntrepriseCivile, nom),
		                    message);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			return null;
		});
	}

	private String getMessageFromTraitementManuel(TraitementManuel msg) throws NoSuchFieldException, IllegalAccessException {
		Class<?> spyClass = msg.getClass();
		Field messageField = spyClass.getDeclaredField("message");
		messageField.setAccessible(true);
		return (String) messageField.get(msg);
	}

	@Test(timeout = 10000L)
	public void testPlusieursEntreprisesRapprochees() throws Exception {

		// Mise en place service mock
		final RegDate dateDeDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDeDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));
			}
		});

		// Création des entreprises

		long noEntrerpise1 = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDeDebut, null, "Synergy truc bidule");
			return entreprise.getNumero();
		});

		long noEntrerpise2 = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDeDebut, null, "Synergy machin chose");
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(1, listEvtInterne.size());
		Assert.assertTrue(listEvtInterne.get(0) instanceof TraitementManuel);
		String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(0));
		Assert.assertEquals(String.format("Plusieurs entreprises non-annulées partagent le même numéro d'entreprise 101202100: ([%d, %d])",
		                                  noEntrerpise1, noEntrerpise2),
		                    message);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testEntrepriseIdentCorrectFormeJurIncompatible() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise
		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = addEtablissement();
			etablissement.setRaisonSociale(nom + "Etab");
			addDomicileEtablissement(etablissement, dateDebut, null, MockCommune.Lausanne);
			return etablissement.getNumero();
		});
		final long noEntreprise = doInNewTransactionAndSession(status -> {
			final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, nom);
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.EI);
			tiersService.addActiviteEconomique(etablissement, entreprise, dateDebut, true);
			return entreprise.getNumero();
		});
		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(1, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof TraitementManuel);
			String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Impossible de rattacher l'entreprise civile n°%d (%s) à l'entreprise n°%s (%s) (%s) " +
					                                  "identifiée sur la base de ses attributs civils [%s, IDE: CHE-999.999.996]: les formes juridiques ne correspondent pas. Arrêt du traitement.",
			                                  noEntrepriseCivile,
			                                  FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
			                                  FormatNumeroHelper.numeroCTBToDisplay(noEntreprise),
			                                  nom,
			                                  FormeJuridiqueEntreprise.EI.getLibelle(),
			                                  nom), message);
		}

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testProtectionContreEvenementDansLePasse() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", date(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Lausanne));
			}
		});

		final long idEntreprise = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			return entreprise.getNumero();
		});

		// Mise en place Translator "espion"
		final SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement

		final EvenementEntreprise event1 = createEvent(1L, noEntrepriseCivile, FOSC_AUTRE_MUTATION, date(2015, 8, 18), TRAITE);
		final EvenementEntreprise event2 = createEvent(2L, noEntrepriseCivile, FOSC_AUTRE_MUTATION, date(2015, 8, 10), A_TRAITER);

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			hibernateTemplate.merge(event1);
			return hibernateTemplate.merge(event2).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		final List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(3, listEvtInterne.size());

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(2L);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final List<EvenementEntrepriseErreur> messages = evt.getErreurs();
			Assert.assertNotNull(messages);
			Assert.assertEquals(1, messages.size());
			Assert.assertEquals(
					String.format(
							"Correction dans le passé: l'événement n°%d [%s] reçu de RCEnt pour l'entreprise civile %d possède une date de valeur antérieure à la date portée par un autre événement reçu avant. " +
									"Traitement automatique impossible.",
							2L, RegDateHelper.dateToDisplayString(date(2015, 8, 10)), noEntrepriseCivile
					),
					messages.get(0).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testProtectionContreEvenementDansLePasseMemeDate() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", date(2000, 1, 1), null, FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Lausanne));
			}
		});

		final long idEntreprise = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			return entreprise.getNumero();
		});

		// Mise en place Translator "espion"
		final SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement
		final Long noEvenement = 12344321L;

		final EvenementEntreprise event1 = createEvent(99999L, noEntrepriseCivile, FOSC_AUTRE_MUTATION, date(2015, 6, 18), TRAITE);
		final EvenementEntreprise event2 = createEvent(noEvenement, noEntrepriseCivile, FOSC_AUTRE_MUTATION, date(2015, 6, 18), A_TRAITER);
		event2.setCorrectionDansLePasse(true);

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			hibernateTemplate.merge(event1);
			return hibernateTemplate.merge(event2).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		final List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(3, listEvtInterne.size());

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final List<EvenementEntrepriseErreur> messages = evt.getErreurs();
			Assert.assertNotNull(messages);
			Assert.assertEquals(1, messages.size());
			Assert.assertEquals(
					String.format(
							"Correction dans le passé: l'événement n°%d [%s] reçu de RCEnt pour l'entreprise civile %d est marqué comme événement de correction dans le passé. " +
									"Traitement automatique impossible.",
							noEvenement, RegDateHelper.dateToDisplayString(date(2015, 6, 18)), noEntrepriseCivile
					),
					messages.get(0).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRaisonIndividuellePureREEIgnoreeNonTraitementManuel() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		final MockEntrepriseCivile entreprise = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE,
		                                                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null,
		                                                                               null, null, null, null);
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entreprise);

			}
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(4, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Aucune entreprise identifiée pour le numéro civil %d ou les attributs civils [%s].", noEntrepriseCivile, nom), message);
		}
		{
			Assert.assertTrue(listEvtInterne.get(1) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(1));
			Assert.assertEquals(
					String.format("L'entreprise civile n°%d est une entreprise individuelle vaudoise. Pas de création.",
					              noEntrepriseCivile), message);
		}
		Assert.assertTrue(listEvtInterne.get(2) instanceof ValideurDebutDeTraitement);
		Assert.assertTrue(listEvtInterne.get(3) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRaisonIndividuellePureREESansFormeLegaleTraitementManuel() throws Exception {

		// Mise en place service mock
		final RegDate dateDebut = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		final MockEntrepriseCivile entreprise = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, nom, dateDebut, null, null,
		                                                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null,
		                                                                               null, null, null, null);
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entreprise);

			}
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Mise en place Translator "espion"
		SpyEvenementEntrepriseTranslatorImpl translator = new SpyEvenementEntrepriseTranslatorImpl();

		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setAudit(audit);
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Verification de l'événement interne créé
		List<EvenementEntrepriseInterne> listEvtInterne = getListeEvtInternesCrees(translator);
		Assert.assertEquals(5, listEvtInterne.size());
		{
			Assert.assertTrue(listEvtInterne.get(0) instanceof MessageSuiviPreExecution);
			String message = getMessageFromMessageSuiviPreExecution((MessageSuiviPreExecution) listEvtInterne.get(0));
			Assert.assertEquals(String.format("Aucune entreprise identifiée pour le numéro civil %d ou les attributs civils [%s].", noEntrepriseCivile, nom), message);
		}
		Assert.assertTrue(listEvtInterne.get(2) instanceof ValideurDebutDeTraitement);
		{
			Assert.assertTrue(listEvtInterne.get(3) instanceof TraitementManuel);
			String message = getMessageFromTraitementManuel((TraitementManuel) listEvtInterne.get(3));
			Assert.assertEquals(
					String.format("L'entreprise Synergy SA (civil: n°%d), domiciliée à Lausanne (VD), n'existe pas à l'IDE ni au RC. Pas de création automatique.",
					              noEntrepriseCivile), message);
		}
		Assert.assertTrue(listEvtInterne.get(4) instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			return null;
		});
	}
}
