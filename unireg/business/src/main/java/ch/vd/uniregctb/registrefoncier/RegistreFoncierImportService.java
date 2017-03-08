package ch.vd.uniregctb.registrefoncier;

import org.quartz.SchedulerException;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;

/**
 * Service qui expose des actions métier sur les données importée hebdomadairement du registre foncier (Capistastra).
 */
public interface RegistreFoncierImportService {

	/**
	 * Efface toutes les mutations associées avec l'import spécifié.
	 * <p/>
	 * <b>Cette méthode ne doit pas être appelée dans un context transactionnel car elle gère elle-même les transactions.</b>
	 *
	 * @param importId      l'id d'un import
	 * @param statusManager un status manager pour suivre l'avancement de l'effacement
	 * @return le nombre de mutations supprimées
	 */
	int deleteAllMutations(long importId, StatusManager statusManager);

	/**
	 * Lance le traitement d'un import du registre foncier.
	 *
	 * @param importId l'id d'un import
	 */
	void startImport(long importId) throws JobAlreadyStartedException, SchedulerException;

	/**
	 * Passe un import (et les mutations) du registre foncier à l'état FORCE (c'est-à-dire qu'il ne sera plus jamais traité, même s'il n'est pas traité entièrement).
	 *
	 * @param importId l'id d'un import
	 */
	void forceImport(long importId);

	/**
	 * Lance le traitement des mutations d'un import du registre foncier.
	 *
	 * @param importId l'id d'un import
	 */
	void startMutations(long importId) throws JobAlreadyStartedException, SchedulerException;

	/**
	 * Passe la mutation spécifiée à l'état FORCE (c'est-à-dire qu'elle ne sera plus jamais traitée).
	 *
	 * @param mutId l'id d'une mutation
	 */
	void forceMutation(long mutId);

	/**
	 * Passe toutes les mutations d'un import du registre foncier à l'état FORCE (c'est-à-dire qu'elles ne seront plus jamais traitées).
	 *
	 * @param importId l'id d'un import
	 */
	void forceAllMutations(long importId);
}
