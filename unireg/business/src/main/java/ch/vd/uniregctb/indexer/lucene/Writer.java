package ch.vd.uniregctb.indexer.lucene;

import java.io.IOException;

import org.apache.lucene.index.Term;

import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerException;

public interface Writer {

	void optimize() throws IndexerException;

	/**
	 * Supprime un document représenté par un {@link ch.vd.uniregctb.indexer.IndexableData}.
	 *
	 * @param indexable l'indexable à supprimer.
	 * @throws ch.vd.uniregctb.indexer.IndexerException en cas d'erreur
	 */
	void remove(IndexableData indexable);

	/**
	 * Supprime un document représenté par ses données brutes.
	 *
	 * @param id        l'id du document lucene
	 * @param typeValue le type de document à supprimer
	 * @throws ch.vd.uniregctb.indexer.IndexerException en cas d'erreur
	 */
	void remove(long id, String typeValue) throws IndexerException;

	/**
	 * Indexe un document représenté par un {@link ch.vd.uniregctb.indexer.IndexableData}.
	 *
	 * @param indexable l'indexable à indexer.
	 * @throws ch.vd.uniregctb.indexer.IndexerException en cas d'erreur
	 */
	void index(IndexableData indexable) throws IndexerException;

	void deleteDocuments(Term term) throws IOException;

	/**
	 * Selon la documentation Lucene : <p>Commits all pending updates (added & deleted documents) to the index, and syncs all referenced index files, such that a reader will see the changes and the index
	 * updates will survive an OS or machine crash or power loss.  Note that this does not wait for any running background merges to finish.  This may be a costly operation, so you should test the cost
	 * in your application and do it only when really necessary.</p> <p> Note that this operation calls Directory.sync on the index files.  That call should not return until the file contents & metadata
	 * are on stable storage. For FSDirectory, this calls the OS's fsync.  But, beware: some hardware devices may in fact cache writes even during fsync, and return before the bits are actually on stable
	 * storage, to give the appearance of faster performance.  If you have such a device, and it does not have a battery backup (for example) then on power loss it may still lose data.  Lucene cannot
	 * guarantee consistency on such devices.  </p>
	 *
	 * @throws IndexerException en cas d'erreur
	 */
	void commit() throws IndexerException;
}
