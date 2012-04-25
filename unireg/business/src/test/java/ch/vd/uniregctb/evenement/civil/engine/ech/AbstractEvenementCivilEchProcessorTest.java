package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.apache.commons.lang.mutable.MutableBoolean;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.interfaces.service.rcpers.ProxyRcPersClientHelper;

public abstract class AbstractEvenementCivilEchProcessorTest extends BusinessTest {
	
	private EvenementCivilNotificationQueueImpl queue;
	protected EvenementCivilEchProcessorImpl processor;
	protected EvenementCivilEchDAO evtCivilDAO;
	protected EvenementCivilEchService evtCivilService;
	protected ProxyRcPersClientHelper rcPersClientHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evtCivilDAO  = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		evtCivilService  = getBean(EvenementCivilEchService.class, "evtCivilEchService");
		rcPersClientHelper = getBean(ProxyRcPersClientHelper.class, "rcPersClientHelper");

		final EvenementCivilEchTranslator translator = getBean(EvenementCivilEchTranslator.class, "evenementCivilEchTranslator");

		queue = new EvenementCivilNotificationQueueImpl(0);
		queue.setEvtCivilService(evtCivilService);
		buildProcessor(translator, false);
		processor.start();
	}

	@Override
	public void onTearDown() throws Exception {
		processor.stop();
		rcPersClientHelper.tearDown();
		super.onTearDown();
	}
	
	protected void buildProcessor(EvenementCivilEchTranslator translator, boolean restart) {
		final EvenementCivilEchProcessorImpl proc = new EvenementCivilEchProcessorImpl();
		proc.setEvtCivilDAO(evtCivilDAO);
		proc.setNotificationQueue(queue);
		proc.setTransactionManager(transactionManager);
		proc.setTranslator(translator);
		proc.setTiersService(tiersService);
		proc.setIndexer(globalTiersIndexer);
		proc.setServiceCivil(serviceCivil);
		if (restart && processor != null) {
			processor.stop();
		}
		processor = proc;
		if (restart) {
			processor.start();
		}
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
