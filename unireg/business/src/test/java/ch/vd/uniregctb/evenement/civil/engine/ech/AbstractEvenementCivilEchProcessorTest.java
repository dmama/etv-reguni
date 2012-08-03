package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.apache.commons.lang.mutable.MutableBoolean;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.metier.MetierService;

public abstract class AbstractEvenementCivilEchProcessorTest extends BusinessTest {
	
	private EvenementCivilNotificationQueueImpl queue;
	protected EvenementCivilEchProcessorImpl processor;
	protected EvenementCivilEchDAO evtCivilDAO;
	protected EvenementCivilEchService evtCivilService;
	protected DataEventService dataEventService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evtCivilDAO  = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		evtCivilService  = getBean(EvenementCivilEchService.class, "evtCivilEchService");
		dataEventService = getBean(DataEventService.class, "dataEventService");

		final EvenementCivilEchTranslator translator = getBean(EvenementCivilEchTranslator.class, "evenementCivilEchTranslator");

		queue = new EvenementCivilNotificationQueueImpl(0);
		queue.setEvtCivilService(evtCivilService);
		if (buildProcessorOnSetup()) {
			buildProcessor(translator, false);
			processor.afterPropertiesSet();
			processor.start();
		}
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
		proc.setDataEventService(dataEventService);
		if (restart && processor != null) {
			processor.stop();
		}
		processor = proc;
		if (restart) {
			processor.afterPropertiesSet();
			processor.start();
		}
	}

	protected static interface StrategyOverridingCallback {
		void overrideStrategies(EvenementCivilEchTranslatorImplOverride translator);
	}

	protected void buildStrategyOverridingTranslatorAndProcessor(boolean restart, StrategyOverridingCallback callback) throws Exception {
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
		translator.setParameters(getBean(EvenementCivilEchStrategyParameters.class, "evenementCivilEchStrategyParameters"));
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
			queue.post(noIndividu, false);

			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (jobDone) {
				// on attend que le traitement se fasse
				while (!jobDone.booleanValue()) {
					jobDone.wait();
				}
			}
		}
		finally {
			processor.unregisterListener(handle);
		}
	}
}
