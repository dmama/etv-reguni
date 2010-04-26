package ch.vd.uniregctb.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.springframework.util.Assert;

/**
 * Wrapper autour d'un index writer lucene qui permet de manipuler directement des objets de type {@link IndexableData}.
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class LuceneWriter extends LuceneEngine {

	private static final Logger LOGGER = Logger.getLogger(LuceneWriter.class);

	/**
	 * L'index writer Lucene
	 */
	private IndexWriter iw;

	/**
	 * Le Directory pour l'index (File ou JDBC)
	 */
	private final Directory directory;

	/**
	 * @param directory le répertoire lucene sur lequel le writer doit travailler
	 * @param createIt  vrai s'il faut créer (ou recréer) le répertoire lucene
	 * @throws IndexerException en cas de problème
	 */
	public LuceneWriter(Directory directory, boolean createIt) throws IndexerException {
		this.directory = directory;

		Analyzer an = getFrenchAnalyzer();
		try {
			createIndexModifier(an, createIt);
		}
		catch (FileNotFoundException e) {
			try {
				createIndexModifier(an, true); // Force creation
			}
			catch (IOException ee) {
				throw new IndexerException(ee);
			}
		}
		catch (IOException e) {
			throw new IndexerException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	private void createIndexModifier(Analyzer an, boolean createIt) throws IOException {
		if (createIt) {
			LOGGER.info("Index created (overwrite mode)");
		}
		iw = new IndexWriter(directory, an, createIt, IndexWriter.MaxFieldLength.LIMITED);
		// [UNIREG-2220] ce paramètre provoque un usage excessif de mémoire et il n'est pas nécessaire que les terms effacés seront flushés lors du close du writer: 
		// iw.setMaxBufferedDeleteTerms(1); // Force le delete a chaque fois
		Assert.isTrue(!createIt || iw.maxDoc() == 0, "L'indexeur n'est pas vide apres l'overwrite");
	}

	@Override
	public void close() throws IndexerException {

		if (iw != null) {
			try {
				iw.close();
			}
			catch (FileNotFoundException e) {
				// Nothing to do, the index is already empty
			}
			catch (IOException e) {
				throw new IndexerException(e);
			}
			finally {
				iw = null;
			}
		}
	}

	public void optimize() throws IndexerException {
		LOGGER.trace("CALL optimize()");
		try {
			iw.optimize();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		LOGGER.trace("END optimize()");
	}

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
	public void commit() throws IndexerException {
		LOGGER.trace("CALL commit()");
		try {
			iw.commit();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		LOGGER.trace("END commit()");
	}

	/**
	 * Génère l'id utilisé par lucene pour identifier un document
	 *
	 * @param typeValue le type de document à indexer
	 * @param id        un id propore au document
	 * @return une chaîne de caractère qui représente l'id du document lucene
	 */
	private String generateDocumentID(String typeValue, long id) {
		return typeValue.toLowerCase() + "-" + id;
	}

	/**
	 * Supprime un document représenté par un {@link IndexableData}.
	 *
	 * @param indexable l'indexable à supprimer.
	 * @throws IndexerException en cas d'erreur
	 */
	public void remove(IndexableData indexable) {
		remove(indexable.getId(), indexable.getType());
	}

	/**
	 * Supprime un document représenté par ses données brutes.
	 *
	 * @param id        l'id du document lucene
	 * @param typeValue le type de document à supprimer
	 * @throws IndexerException en cas d'erreur
	 */
	public void remove(long id, String typeValue) throws IndexerException {

		try {
			// Delete old document
			if (typeValue != null) {
				iw.deleteDocuments(new Term(F_DOCID, generateDocumentID(typeValue, id)));
			}
			else {
				iw.deleteDocuments(new Term(F_ENTITYID, Long.toString(id)));
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	/**
	 * Indexe un document représenté par un {@link IndexableData}.
	 *
	 * @param indexable l'indexable à indexer.
	 * @throws IndexerException en cas d'erreur
	 */
	public void index(IndexableData indexable) throws IndexerException {

		Assert.notNull(iw);

		try {
			Document d = indexable.asDoc();
			Assert.notNull(d);
			iw.addDocument(d);
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	public void deleteDocuments(Term term) throws IOException {
		iw.deleteDocuments(term);
	}

}
