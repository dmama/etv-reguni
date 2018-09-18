package ch.vd.unireg.indexer.tiers;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.tiers.Tiers;

/**
 * Service spécialisé pour la mise-à-jour de l'indexe Lucene par rapport aux Tiers.
 */
public interface GlobalTiersIndexer {

	String SERVICE_NAME = "GlobalTiersIndex";

	/**
	 * Efface l'index.
	 */
	void overwriteIndex();

	/**
	 * Demande l'indexation ou la ré-indexation d'un tiers.
	 * <p/>
	 * Cette méthode retourne immédiatement : l'indexation proprement dites est déléguée à un thread asynchrone.
	 *
	 * @param id l'id du tiers à indexer
	 */
	void schedule(long id);

	/**
	 * Demande l'indexation ou la ré-indexation de plusieurs tiers.
	 * <p/>
	 * Cette méthode retourne immédiatement : l'indexation proprement dites est déléguée à un thread asynchrone.
	 *
	 * @param ids les ids des tiers à indexer
	 */
	void schedule(Collection<Long> ids);

	/**
	 * Attends que tous les tiers dont l'indexation a été demandée aient été indexés.
	 * <p>
	 * Cette méthode bloque donc tant que la queue d'indexation est pleine.
	 */
	void sync();

	enum Mode {
		/**
		 * Réindexe toute la population spécifiée en faisant table rase des données de l'indexe.
		 */
		FULL,
		/**
		 * Réindexe toute la population spécifiée en mettant-à-jour incrémentalement les données de l'indexe.
		 */
		FULL_INCREMENTAL,
		/**
		 * Réindexe les tiers qui manquent et supprime ceux en trop (par rapport à la DB)
		 */
		MISSING_ONLY,
		/**
		 * Réindexe les tiers flaggés comme <i>dirty</i> dans la DB.
		 */
		DIRTY_ONLY;
	}

	/**
	 * Réindexe toute la base de données (1 thread, mode FULL avec préfetch des individus).
	 *
	 * @throws ch.vd.unireg.indexer.IndexerException
	 *          si l'indexation n'a pas pu être faite.
	 * @return le nombre de tiers indexés
	 */
	int indexAllDatabase() throws IndexerException;

	/**
	 * Indexe ou réindexe tout ou partie de la base de données.
	 *
	 * @param mode          le mode d'indexation voulu.
	 * @param nbThreads     le nombre de threads simultanés utilisés pour indexer la base
	 * @param statusManager un status manager pour suivre l'évolution de l'indexation (peut être nul)
	 * @return le nombre de tiers indexés
	 * @throws ch.vd.unireg.indexer.IndexerException si l'indexation n'a pas pu être faite.
	 */
	int indexAllDatabase(@NotNull Mode mode, int nbThreads, @Nullable StatusManager statusManager) throws IndexerException;

	/**
	 * Indexe les tiers spécifié.
	 *
	 * @param tiers            les tiers à indexer
	 * @param removeBefore     si <b>vrai</b> les données du tiers seront supprimée de l'index avant d'être réinsérée; si <b>false</b> les données seront simplement ajoutées.
	 * @param followDependents si <b>vrai</b> les tiers liés (ménage commun, ...) seront aussi indexées.
	 * @throws IndexerBatchException en cas d'exception lors de l'indexation d'un ou plusieurs tiers. La méthode essaie d'indexer tous les tiers dans tous les cas, ce qui veut dire que si le premier tiers lève une exception, les tiers suivants seront
	 *                               quand même indexés.
	 */
	void indexTiers(@NotNull List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException;

	/**
	 * <b>Note :</b> le switch n'est actif que sur le thread courant
	 * @return le switch de gestion pour savoir si l'indexation doit se faire à la volée ou si elle sera faite <i>a posteriori</i>
	 */
	Switchable onTheFlyIndexationSwitch();

	/**
	 * @return le nombre de tiers actuellement en attente d'indexation dans le mode "on-the-fly"
	 */
	int getOnTheFlyQueueSize();

	/**
	 * @return le nombre de threads actuellement activés pour le traitement des indexations "on-the-fly"
	 */
	int getOnTheFlyThreadNumber();
}
