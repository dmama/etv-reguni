package ch.vd.uniregctb.extraction;

import ch.vd.uniregctb.common.StatusManager;

/**
 * Classe de base des implémentations des extracteur comprenant la gestion du StatusManager
 */
public abstract class BaseExtractorImpl implements Extractor {

	private boolean interrupted;
	private StatusManager statusManager;
	private String runningMessage;
	private Integer percentProgression;

	@Override
	public void interrupt() {
		interrupted = true;
	}

	@Override
	public boolean isInterrupted() {
		return interrupted;
	}

	public String getRunningMessage() {
		return runningMessage;
	}

	public Integer getPercentProgression() {
		return percentProgression;
	}

	/**
	 * Monitoring de l'avancement de l'extraction
	 */
	private final class ExtractorStatusManager implements StatusManager {

		@Override
		public boolean interrupted() {
			return isInterrupted();
		}

		@Override
		public void setMessage(String msg) {
			setRunningMessage(msg);
		}

		@Override
		public void setMessage(String msg, int percentProgression) {
			setRunningMessage(msg, percentProgression);
		}
	}

	protected void setRunningMessage(String msg) {
		this.runningMessage = msg;
		this.percentProgression = null;
	}

	protected void setRunningMessage(String msg, int percentProgression) {
		this.runningMessage = msg;
		this.percentProgression = percentProgression;
	}

	public final synchronized StatusManager getStatusManager() {
		if (this.statusManager == null) {
			this.statusManager = new ExtractorStatusManager();
		}
		return statusManager;
	}

	@Override
	public String toString() {
		return getExtractionName();
	}
}
