package ch.vd.unireg.evenement.civil.engine.ech;

import org.apache.commons.lang3.mutable.MutableBoolean;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.metier.MetierService;

public abstract class AbstractEvenementCivilEchProcessorTest extends BusinessTest {
	
	private EvenementCivilNotificationQueueImpl queue;
	protected EvenementCivilEchProcessorImpl processor;
	protected EvenementCivilEchDAO evtCivilDAO;
	protected EvenementCivilEchService evtCivilService;
	protected CivilDataEventService dataEventService;
	protected ModificationInterceptor mainInterceptor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evtCivilDAO  = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		evtCivilService  = getBean(EvenementCivilEchService.class, "evtCivilEchService");
		dataEventService = getBean(CivilDataEventService.class, "civilDataEventService");
		mainInterceptor = getBean(ModificationInterceptor.class, "modificationInterceptor");

		final EvenementCivilEchTranslator translator = getBean(EvenementCivilEchTranslator.class, "evenementCivilEchTranslator");

		queue = new EvenementCivilNotificationQueueImpl(0);
		queue.setTransactionManager(transactionManager);
		queue.setEvtCivilService(evtCivilService);
		if (buildProcessorOnSetup()) {
			buildProcessor(translator, false);
			processor.afterPropertiesSet();
			processor.start();
		}
		queue.afterPropertiesSet();
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Override
	public void onTearDown() throws Exception {
		if (processor != null) {
			processor.stop();
			processor = null;
		}
		queue.destroy();
		super.onTearDown();
	}
	
	protected void buildProcessor(EvenementCivilEchTranslator translator, boolean restart) throws Exception {
		final EvenementCivilEchProcessorImpl proc = new EvenementCivilEchProcessorImpl();
		proc.setEvtCivilDAO(evtCivilDAO);
		proc.setNotificationQueue(queue);
		proc.setTransactionManager(transactionManager);
		proc.setTranslator(translator);
		proc.setTiersService(tiersService);
		proc.setIndexer(globalTiersIndexer);
		proc.setServiceCivil(serviceCivil);
		proc.setCivilDataEventService(dataEventService);
		proc.setMainInterceptor(mainInterceptor);
		proc.setParentesSynchronizerInterceptor(parentesSynchronizer);
		proc.setAudit(audit);
		if (restart && processor != null) {
			processor.stop();
		}
		processor = proc;
		if (restart) {
			processor.afterPropertiesSet();
			processor.start();
		}
	}

	protected interface StrategyOverridingCallback {
		void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator);
	}

	protected void buildStrategyOverridingTranslatorAndProcessor(boolean restart, StrategyOverridingCallback callback) throws Exception {
		final EvenementCivilEchTranslatorImplOverride translator = new EvenementCivilEchTranslatorImplOverride();
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setCivilDataEventService(getBean(CivilDataEventService.class, "civilDataEventService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setIndexer(globalTiersIndexer);
		translator.setMetierService(getBean(MetierService.class, "metierService"));
		translator.setServiceCivilService(serviceCivil);
		translator.setServiceInfrastructureService(serviceInfra);
		translator.setTiersDAO(tiersDAO);
		translator.setTiersService(tiersService);
		translator.setParameters(getBean(EvenementCivilEchStrategyParameters.class, "evenementCivilEchStrategyParameters"));
		translator.setAudit(audit);
		translator.afterPropertiesSet();
		callback.overrideStrategies(translator);
		buildProcessor(translator, restart);
	}

	protected void traiterEvenements(final long noIndividu) throws InterruptedException {
		// on se met en position pour voir quand le traitement aura été effectué
		final MutableBoolean jobDone = new MutableBoolean(false);
		final EvenementCivilEchProcessor.ListenerHandle handle = processor.registerListener(new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividuTraite) {
				if (noIndividuTraite == noIndividu) {
					setAndNotify();
				}
			}

			@Override
			public void onStop() {
				setAndNotify();
			}

			private void setAndNotify() {
				synchronized (jobDone) {
					jobDone.setValue(true);
					jobDone.notifyAll();
				}
			}
		});

		try {
			// notification d'arrivée d'événement sur l'individu
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);

			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (jobDone) {
				// on attend que le traitement se fasse
				while (!jobDone.booleanValue()) {
					jobDone.wait();
				}
			}
		}
		finally {
			handle.unregister();
		}

		// après l'exécution d'un ou plusieurs événements civils en test, il est préférable d'attendre que
		// toutes les indexations on-the-fly soient terminées (car le traitement a lieu sur un thread séparé
		// dans lequel ce type d'indexation est activé par défaut)
		globalTiersIndexer.sync();
	}
}
