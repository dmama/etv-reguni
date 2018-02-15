package ch.vd.unireg.registrefoncier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.scheduler.JobAlreadyStartedException;

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

	/**
	 * Recherche l'entité RF qui correspond à la mutation spécifié.
	 *
	 * @param type        le type de la mutation.
	 * @param idRF        l'id RF métier de l'entité
	 * @param versionIdRF le versionId de l'entité (seulement nécessaire pour les servitudes)
	 * @return l'entité correspondante, ou <b>null</b> si non trouvé
	 */
	@Nullable
	HibernateEntity findEntityForMutation(@NotNull TypeEntiteRF type, @NotNull String idRF, @Nullable String versionIdRF);

	/**
	 * Détermine s'il y a un import de servitudes à traiter qui correspond à l'import spécifié.
	 * <p/>
	 * Note: cette méthode peut être appelée en dehors d'un context transactionnel.
	 *
	 * @param importId un id d'un import principal ou de servitudes
	 * @return l'id de l'import des servitudes à traiter qui correspond à l'import principal spécifié; <i>null</i> autrement.
	 */
	@Nullable
	Long findMatchingImportServitudesToProcess(long importId);
}
