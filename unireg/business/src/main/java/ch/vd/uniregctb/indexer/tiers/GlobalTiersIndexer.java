package ch.vd.uniregctb.indexer.tiers;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;

/**
 * Service spécialisé pour la mise-à-jour de l'indexe Lucene par rapport aux Tiers.
 */
public interface GlobalTiersIndexer {

	/**
	 * Efface l'index.
	 */
	public void overwriteIndex();

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

	public enum Mode {
		FULL,
		INCREMENTAL,
		DIRTY_ONLY
	}

	/**
	 * Réindexe toute la base de données (1 thread, mode FULL avec préfetch des individus).
	 *
	 * @throws ch.vd.uniregctb.indexer.IndexerException
	 *          si l'indexation n'a pas pu être faite.
	 * @return le nombre de tiers indexés
	 */
	int indexAllDatabase() throws IndexerException;

	/**
	 * Indexe ou réindexe tout ou partie de la base de données.
	 *
	 * @param statusManager             un status manager pour suivre l'évolution de l'indexation (peut être nul)
	 * @param nbThreads                 le nombre de threads simultanés utilisés pour indexer la base
	 * @param mode                      le mode d'indexation voulu.
	 * @param prefetchPMs               détermine si les PMs doivent être préchargés en vrac
	 * @return le nombre de tiers indexés
	 * @throws ch.vd.uniregctb.indexer.IndexerException
	 *          si l'indexation n'a pas pu être faite.
	 */
	int indexAllDatabase(@Nullable StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchPMs) throws IndexerException;

	/**
	 * Flag qui indique si l'indexation doit se faire a la volée ou si elle sera faite a posteriori.
	 * <p/>
	 * Note: cette valeur est valable <b>pour le thread courant</b>.
	 *
	 * @return <b>vrai</b> si l'indexation est faite à la volée ou <b>faux</b> si elle est faite à postériori.
	 */
	boolean isOnTheFlyIndexation();

	/**
	 * Active ou désactive l'indexation <b>pour le thread courant</b>.
	 *
	 * @param onTheFlyIndexation <b>vrai</b> si l'indexation doit se faire à la volée ou <b>faux</b> si elle sera faite à postériori.
	 */
	void setOnTheFlyIndexation(boolean onTheFlyIndexation);

	/**
	 * @return le nombre de tiers actuellement en attente d'indexation dans le mode "on-the-fly"
	 */
	int getOnTheFlyQueueSize();

	/**
	 * @return le nombre de threads actuellement activés pour le traitement des indexations "on-the-fly"
	 */
	int getOnTheFlyThreadNumber();
}
