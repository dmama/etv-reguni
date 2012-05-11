package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchContext;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneComposite;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.testing.Testing;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("JavaDoc")
public class EvenementCivilEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testEvenementExceptionDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoExceptionDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.ERROR, erreur0.getType());
				assertEquals("Exception de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementTraiteAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementRedondantAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementTraiteSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				assertEmpty(evt.getErreurs());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementRedondantSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
				assertEmpty(evt.getErreurs());
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testTraitementEvenementIndexationPureSiErreur() throws Exception {
		
		final long noIndividu = 316547256L;
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1976, 8, 22), "Kaderate", "Yamamoto", true); 
			}
		});
		
		// aucune mise en place fiscale -> le souci lors du traitement du mariage, ce sera justement que le tiers n'existe pas...
		
		// création des événements sur cet individu : 1 événement qui part en erreur et un événement d'indexation pure à une date postérieure (ce dernier doit être traité quand-même)
		final long evtErreurId = 32657743L;
		final long evtIndexationId = 423677342L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final RegDate dateMariage = date(2000, 1, 1);
				
				// événement qui partira en erreur
				final EvenementCivilEch evtErreur = new EvenementCivilEch();
				evtErreur.setId(evtErreurId);
				evtErreur.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtErreur.setDateEvenement(dateMariage);
				evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
				evtErreur.setNumeroIndividu(noIndividu);
				evtErreur.setType(TypeEvenementCivilEch.MARIAGE);
				hibernateTemplate.merge(evtErreur);
				
				// événement d'indexation pure (obtention d'origine)
				final EvenementCivilEch evtIndexation = new EvenementCivilEch();
				evtIndexation.setId(evtIndexationId);
				evtIndexation.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtIndexation.setDateEvenement(dateMariage.addYears(1));              // après celui qui partira en erreur, donc
				evtIndexation.setEtat(EtatEvenementCivil.A_TRAITER);
				evtIndexation.setNumeroIndividu(noIndividu);
				evtIndexation.setType(TypeEvenementCivilEch.OBENTION_DROIT_CITE);
				hibernateTemplate.merge(evtIndexation);
				return null;
			}
		});

		// traitement de l'événement, passage en erreur et traitement de l'indexation pure quand-même (si tout va bien...)
		traiterEvenements(noIndividu);
		
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evtErreur = evtCivilDAO.get(evtErreurId);
				assertNotNull(evtErreur);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evtErreur.getEtat());

				final EvenementCivilEch evtIndexation = evtCivilDAO.get(evtIndexationId);
				assertNotNull(evtIndexation);
				assertEquals(EtatEvenementCivil.TRAITE, evtIndexation.getEtat());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testMiseEnAttente() throws Exception {

		final long noIndividu = 316547256L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1976, 8, 22), "Kaderate", "Yamamoto", true);
			}
		});

		// aucune mise en place fiscale -> le souci lors du traitement du mariage, ce sera justement que le tiers n'existe pas...

		// création des événements sur cet individu : 1 événement qui part en erreur et un événement d'indexation pure à une date postérieure (ce dernier doit être traité quand-même)
		final long evtErreurId = 32657743L;
		final long evtAttenteId = 423677342L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final RegDate dateMariage = date(2000, 1, 1);

				// événement qui partira en erreur
				final EvenementCivilEch evtErreur = new EvenementCivilEch();
				evtErreur.setId(evtErreurId);
				evtErreur.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtErreur.setDateEvenement(dateMariage);
				evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
				evtErreur.setNumeroIndividu(noIndividu);
				evtErreur.setType(TypeEvenementCivilEch.MARIAGE);
				hibernateTemplate.merge(evtErreur);

				// événement qui sera mis en attente
				final EvenementCivilEch evtAttente = new EvenementCivilEch();
				evtAttente.setId(evtAttenteId);
				evtAttente.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtAttente.setDateEvenement(dateMariage.addYears(1));              // après celui qui partira en erreur, donc
				evtAttente.setEtat(EtatEvenementCivil.A_TRAITER);
				evtAttente.setNumeroIndividu(noIndividu);
				evtAttente.setType(TypeEvenementCivilEch.DIVORCE);
				hibernateTemplate.merge(evtAttente);
				return null;
			}
		});

		// traitement de l'événement, passage en erreur et traitement de l'indexation pure quand-même (si tout va bien...)
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evtErreur = evtCivilDAO.get(evtErreurId);
				assertNotNull(evtErreur);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evtErreur.getEtat());

				final EvenementCivilEch evtIndexation = evtCivilDAO.get(evtAttenteId);
				assertNotNull(evtIndexation);
				assertEquals(EtatEvenementCivil.EN_ATTENTE, evtIndexation.getEtat());
				return null;
			}
		});
	}

	private interface AfterHandleCallback {
		void checkEvent(EvenementCivilEch evt);
	}

	private void traiterEvenementTesting(final long noEventTesting, final AfterHandleCallback afterHandleCallback) throws Exception {
		final long noIndividu = 12345L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 1, 2), "Anoa", "Yamomoto", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addHabitant(noIndividu);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long testingId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(noEventTesting);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(date(2000, 1, 1));
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(testingId);
				assertNotNull(evt);
				afterHandleCallback.checkEvent(evt);
				return null;
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testEvenementCompositeEtTransactionAvecRuntimeException() throws Exception {
		doTestEvenementCompositeEtTransaction(new Handler() {
			@Override
			public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				throw new RuntimeException("Boom!");
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testEvenementCompositeEtTransactionAvecCheckedException() throws Exception {
		doTestEvenementCompositeEtTransaction(new Handler() {
			@Override
			public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
				throw new EvenementCivilException("Boom!");
			}
		});
	}
	
	private static interface Handler {
		HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException;
	}

	/**
	 * Le handler sera appelé dans l'événement interne suivant celui qui a créé un nouveau contribuable
	 * @param handler handler
	 */
	private void doTestEvenementCompositeEtTransaction(final Handler handler) throws Exception {
		
		final long noIndividu = 25614312L;
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		// stratégie qui génère un événement composite qui appelle le handler sur le deuxième traitement
		// après avoir créé un nouveau contribuable sur le premier
		final EvenementCivilEchTranslationStrategy strategy = new EvenementCivilEchTranslationStrategy() {
			@Override
			public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilEchContext context, EvenementCivilOptions options) throws EvenementCivilException {

				// un événement qui crée un truc en base
				final EvenementCivilInterne naissance = new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						final PersonnePhysique pp = new PersonnePhysique(true);
						pp.setNumeroIndividu(getNoIndividu());
						context.getTiersDAO().save(pp);
						return HandleStatus.TRAITE;
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}

					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				};

				// un événement qui explose avec une erreur métier
				final EvenementCivilInterne boom = new EvenementCivilInterne(event, context, options) {
					@NotNull
					@Override
					public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
						return handler.handle(warnings);
					}

					@Override
					protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
					}

					@Override
					protected boolean isContribuableObligatoirementConnuAvantTraitement() {
						return false;
					}
				};

				return new EvenementCivilInterneComposite(event, context, options, naissance, boom);
			}

			@Override
			public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilEchContext context) throws EvenementCivilException {
				return false;
			}
		};

		final EvenementCivilEchTranslatorImplOverride translator = new EvenementCivilEchTranslatorImplOverride();
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setIndexer(globalTiersIndexer);
		translator.setMetierService(getBean(MetierService.class, "metierService"));
		translator.setServiceCivilService(serviceCivil);
		translator.setServiceInfrastructureService(serviceInfra);
		translator.setTiersDAO(tiersDAO);
		translator.setTiersService(tiersService);
		translator.afterPropertiesSet();

		translator.overrideStrategy(TypeEvenementCivilEch.TESTING, ActionEvenementCivilEch.PREMIERE_LIVRAISON, strategy);
		buildProcessor(translator, true);
		
		// construction de l'événement en base
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1367813456723L);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setDateEvenement(RegDate.get());
				return hibernateTemplate.merge(evt).getId();
			}
		});
		
		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification que rien n'a été committé en base (autre que les messages d'erreur, bien-sûr)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				
				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				assertNotNull(erreur);
				assertEquals("Boom!", erreur.getMessage());
				
				// la personne physique ne doit pas avoir été créée
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNull("La personne physique n'aurait pas dû survivre à la transaction", pp);
				return null;
			}
		});
	}
	
	@Test(timeout = 10000L)
	public void testListenerSansEvenement() throws Exception {

		final long noIndividu = 346783L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		// lancement du traitement des événements en queue pour cet individu
		traiterEvenements(noIndividu);
		
		// mais l'individu n'en a pas, justement -> si la mécanique de listener ne fonctionne pas dans ce cas là
		// alors la méthode traiterEvenements() ne reviendra jamais et c'est le timeout du test qui va sauter
	}
	
	@Test(timeout = 10000L)
	public void testListenerAvecEvenementTousTraites() throws Exception {

		final long noIndividu = 423782690773L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		// création de quelques événements sur cet individu, déjà tous traités
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				// événement déja traité
				final EvenementCivilEch evtTraite = new EvenementCivilEch();
				evtTraite.setId(3523732L);
				evtTraite.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtTraite.setDateEvenement(RegDate.get());
				evtTraite.setEtat(EtatEvenementCivil.TRAITE);
				evtTraite.setNumeroIndividu(noIndividu);
				evtTraite.setType(TypeEvenementCivilEch.TESTING);
				hibernateTemplate.merge(evtTraite);

				// événement forcé (= déjà traité aussi)
				final EvenementCivilEch evtForce = new EvenementCivilEch();
				evtForce.setId(3427842L);
				evtForce.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtForce.setDateEvenement(RegDate.get());
				evtForce.setEtat(EtatEvenementCivil.FORCE);
				evtForce.setNumeroIndividu(noIndividu);
				evtForce.setType(TypeEvenementCivilEch.TESTING);
				hibernateTemplate.merge(evtForce);

				return null;
			}
		});

		// lancement du traitement des événements en queue pour cet individu
		traiterEvenements(noIndividu);

		// mais l'individu n'en a pas à traiter, justement -> si la mécanique de listener ne fonctionne pas dans ce cas là
		// alors la méthode traiterEvenements() ne reviendra jamais et c'est le timeout du test qui va sauter
	}
	
	@Test
	public void testListenerHandleUsage() throws Exception {
		// handle null
		try {
			processor.unregisterListener(null);
			fail("Aurait dû être considéré comme invalide");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Invalid handle", e.getMessage());
		}

		// handle non null mais bidon
		try {
			processor.unregisterListener(new EvenementCivilEchProcessor.ListenerHandle() {});
			fail("Aurait dû être considéré comme invalide");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Invalid handle", e.getMessage());
		}
		
		// handle obtenu normalement
		final EvenementCivilEchProcessor.Listener listener = new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividu) {
			}

			@Override
			public void onStop() {
			}
		};
		final EvenementCivilEchProcessor.ListenerHandle handle = processor.registerListener(listener);
		processor.unregisterListener(handle);
	}

	@Test(timeout = 10000L)
	public void testListenerRegistration() throws Exception {

		final long noIndividu = 467278456783L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 4, 12), "Lara", "Clette", false);
			}
		});

		final MutableBoolean recu = new MutableBoolean(false);
		final EvenementCivilEchProcessor.Listener listener = new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividu) {
				recu.setValue(true);
			}

			@Override
			public void onStop() {
			}
		};

		// traitement des événements (= aucun) de l'individu
		traiterEvenements(noIndividu);
		
		// notre listener n'a rien reçu
		assertFalse(recu.booleanValue());
		
		// après insertion du listener, il devrait recevoir les nouveaux événements
		final EvenementCivilEchProcessor.ListenerHandle handle = processor.registerListener(listener);
		assertFalse(recu.booleanValue());
		traiterEvenements(noIndividu);
		assertTrue(recu.booleanValue());

		// après désactivation du listener, il ne devrait plus rien recevoir
		recu.setValue(false);
		processor.unregisterListener(handle);
		assertFalse(recu.booleanValue());
		traiterEvenements(noIndividu);
		assertFalse(recu.booleanValue());
	}
}
