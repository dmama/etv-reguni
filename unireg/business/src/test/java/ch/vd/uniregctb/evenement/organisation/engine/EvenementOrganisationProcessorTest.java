package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslatorImpl;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.Indexation;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.FOSC;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.EN_ERREUR;
import static ch.vd.uniregctb.type.TypeEvenementOrganisation.FOSC_COMMUNICATION_DANS_FAILLITE;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class EvenementOrganisationProcessorTest extends AbstractEvenementOrganisationProcessorTest {

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
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Création de l'événement
		final Long evtId = 12344321L;

		final EvenementOrganisation event = createEvent(evtId, noOrganisation, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "abcdefgh");

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
		Assert.assertTrue(translator.getCreatedEvent() instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());
				                             return null;
			                             }
		                             }
		);
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
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.afterPropertiesSet();

		buildProcessor(translator);

		// Persistence événement
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				// Création de l'événement
				final Long evtId = 12344321L;
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, FOSC_COMMUNICATION_DANS_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "abcdefgh");
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Verification de l'événement interne créé
		Assert.assertTrue(translator.getCreatedEvent() instanceof Indexation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
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
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final EvenementOrganisation eventCreation = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), EN_ERREUR, FOSC, "abcdefgh");
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
				EvenementOrganisation event = evtOrganisationDAO.get(evtId);
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
				EvenementOrganisation evenement = evtOrganisationDAO.get(evtId);
				Assert.assertEquals(17, evenement.getErreurs().size());
				StringBuilder orderSignature = new StringBuilder();
				for (EvenementOrganisationErreur err : evenement.getErreurs()) {
					orderSignature.append(err.getMessage());
				}
				Assert.assertEquals("4567891011121314151617181920", orderSignature.toString());
			}
		});
	}
}
