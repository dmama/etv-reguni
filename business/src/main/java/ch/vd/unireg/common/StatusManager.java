package ch.vd.unireg.common;

import ch.vd.shared.batchtemplate.Interruptible;

/**
 * Interface des StatusManagers
 */
public interface StatusManager extends Interruptible {

	/**
	 * @param message nouveau message de progression à assigner au job
	 */
	void setMessage(String message);

	/**
	 * @param message nouveau message de progression à assigner au job
	 * @param percentProgression nouveau pourcentage de progression à assigner au job
	 */
	void setMessage(String message, int percentProgression);
}
