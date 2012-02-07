package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.apache.commons.lang.mutable.MutableBoolean;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;

public abstract class AbstractEvenementCivilEchProcessorTest extends BusinessTest {
	
	private EvenementCivilNotificationQueueImpl queue;
	private EvenementCivilEchProcessor processor;
	protected EvenementCivilEchDAO evtCivilDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evtCivilDAO  = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		final EvenementCivilEchTranslator translator = getBean(EvenementCivilEchTranslator.class, "evenementCivilEchTranslator");

		queue = new EvenementCivilNotificationQueueImpl();
		queue.setEvtCivilDAO(evtCivilDAO);
		queue.setHibernateTemplate(hibernateTemplate);
		queue.setTransactionManager(transactionManager);
		queue.afterPropertiesSet();

		buildProcessor(translator, false);
		processor.start();
	}

	@Override
	public void onTearDown() throws Exception {
		processor.stop();
		super.onTearDown();
	}
	
	protected void buildProcessor(EvenementCivilEchTranslator translator, boolean restart) {
		final EvenementCivilEchProcessor proc = new EvenementCivilEchProcessor();
		proc.setEvtCivilDAO(evtCivilDAO);
		proc.setNotificationQueue(queue);
		proc.setTransactionManager(transactionManager);
		proc.setTranslator(translator);
		proc.setTiersService(tiersService);
		proc.setIndexer(globalTiersIndexer);
		if (restart && processor != null) {
			processor.stop();
		}
		processor = proc;
		if (restart) {
			processor.start();
		}
	}
	
	protected void traiterEvenement(long noIndividu, final long noEvenement) throws InterruptedException {
		// on se met en position pour voir quand le traitement aura été effectué
		final MutableBoolean jobDone = new MutableBoolean(false);
		processor.setMonitor(new EvenementCivilEchProcessor.ProcessingMonitor() {
			@Override
			public void onProcessingEnd(long evtId) {
				if (evtId == noEvenement) {
					synchronized(jobDone) {
						jobDone.setValue(true);
						jobDone.notifyAll();
					}
				}
			}
		});

		// notification d'arrivée d'événement sur l'individu
		queue.add(noIndividu);

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (jobDone) {
			// on attend que le traitement se fasse
			while (!jobDone.booleanValue()) {
				jobDone.wait();
			}
		}
	}
}
