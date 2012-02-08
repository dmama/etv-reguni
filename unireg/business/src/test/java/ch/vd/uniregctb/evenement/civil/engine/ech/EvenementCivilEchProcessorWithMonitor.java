package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;

/**
 * Classe de test qui permet d'introduire un {@link ProcessingMonitor moniteur} notifié à fin du traitement de chaque événement civil
 */
public class EvenementCivilEchProcessorWithMonitor extends EvenementCivilEchProcessor {

	private ProcessingMonitor monitor;

	/**
	 * Interface utilisable afin de réagir au traitement d'un événement civil
	 */
	public static interface ProcessingMonitor {
		/**
		 * Appelé à la fin du traitement de l'événement identifié
		 * @param evtId identifiant de l'événement civil pour lequel le traitement vient de se terminer
		 */
		void onProcessingEnd(long evtId);
	}

	public void setMonitor(ProcessingMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	protected boolean processEventAndDoPostProcessingOnError(EvenementCivilNotificationQueue.EvtCivilInfo evt, List<EvenementCivilNotificationQueue.EvtCivilInfo> evts, int pointer) {
		try {
			return super.processEventAndDoPostProcessingOnError(evt, evts, pointer);
		}
		finally {
			if (monitor != null) {
				monitor.onProcessingEnd(evt.idEvenement);
			}
		}
	}
}
