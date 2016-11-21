package ch.vd.uniregctb.registrefoncier;

import org.quartz.SchedulerException;

import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;

/**
 * Service qui expose des actions métier sur les données importée hebdomadairement du registre foncier (Capistastra).
 */
public interface RegistreFoncierService {

	/**
	 * Efface toutes les mutations associées avec l'import spécificé.
	 *
	 * @param importId l'id d'un import
	 * @return le nombre de mutations supprimées
	 */
	int deleteExistingMutations(long importId);

	/**
	 * Lance le traitement d'un import du registre foncier.
	 *
	 * @param importId l'id d'un import
	 */
	void startImport(long importId) throws JobAlreadyStartedException, SchedulerException;

	/**
	 * Passe un import du registre foncier à l'état FORCE (c'est-à-dire qu'il ne sera plus jamais traité, bien qu'il ne soit pas traité entièrement).
	 *
	 * @param importId l'id d'un import
	 */
	void forceImport(long importId);
}
