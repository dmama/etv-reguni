package ch.vd.unireg.indexer.messageidentification;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.indexer.IndexerException;

public interface GlobalMessageIdentificationIndexer {

	/**
	 * Efface l'index.
	 */
	void overwriteIndex();

	/**
	 * Demande l'indexation ou la ré-indexation d'un message de demande d'identification.
	 * @param id l'id du message
	 * @see ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable
	 */
	void reindex(long id);

	/**
	 * Réindexe toute la base de données (<i>n</i> threads).
	 * @param statusManager un status manager pour suivre l'évolution de l'indexation (peut être nul)
	 * @param nbThreads le nombre de threads du traitement
	 * @return le nombre de messages indexés
	 * @throws ch.vd.unireg.indexer.IndexerException si l'indexation n'a pas pu être faite.
	 */
	int indexAllDatabase(@Nullable StatusManager statusManager, int nbThreads) throws IndexerException;
}
