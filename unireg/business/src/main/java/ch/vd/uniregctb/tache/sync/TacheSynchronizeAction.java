package ch.vd.uniregctb.tache.sync;

/**
 * Interface spécifique des actions de synchronisation qui ne touchent que les tâches d'envoi et d'annulation de document
 * (afin de pouvoir les filtrer si nécessaire par rapport à une période fiscale)
 */
public interface TacheSynchronizeAction extends SynchronizeAction {

	/**
	 * @return la période fiscale concernée par la tâche d'envoi ou d'annulation
	 */
	int getPeriodeFiscale();
}
