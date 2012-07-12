package ch.vd.uniregctb.indexer.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.springframework.util.Assert;

import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.OurOwnFrenchAnalyzer;

/**
 * Wrapper autour d'un index writer lucene qui permet de manipuler directement des objets de type {@link ch.vd.uniregctb.indexer.IndexableData}.
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
class LuceneWriter implements Writer {

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
	 * @throws ch.vd.uniregctb.indexer.IndexerException en cas de problème
	 */
	public LuceneWriter(Directory directory, boolean createIt) throws IndexerException {
		this.directory = directory;
		openWriter(createIt);
	}

	private void openWriter(boolean createIt) {
		Analyzer an = new OurOwnFrenchAnalyzer();
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

	private void createIndexModifier(Analyzer an, boolean createIt) throws IOException {
		if (createIt) {
			LOGGER.info("Index created (overwrite mode)");
		}
		iw = new IndexWriter(directory, an, createIt, IndexWriter.MaxFieldLength.LIMITED);
		// [UNIREG-2220] ce paramètre provoque un usage excessif de mémoire et il n'est pas nécessaire que les terms effacés seront flushés lors du close du writer: 
		// iw.setMaxBufferedDeleteTerms(1); // Force le delete a chaque fois
		Assert.isTrue(!createIt || iw.maxDoc() == 0, "L'indexeur n'est pas vide apres l'overwrite");
	}

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

	@Override
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

	@Override
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
		return typeValue.toLowerCase() + '-' + id;
	}

	@Override
	public void remove(IndexableData indexable) {
		remove(indexable.getId(), indexable.getType());
	}

	@Override
	public void remove(long id, String typeValue) throws IndexerException {

		try {
			// Delete old document
			if (typeValue != null) {
				iw.deleteDocuments(new Term(LuceneHelper.F_DOCID, generateDocumentID(typeValue, id)));
			}
			else {
				iw.deleteDocuments(new Term(LuceneHelper.F_ENTITYID, Long.toString(id)));
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	@Override
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

	@Override
	public void deleteDocuments(Term term) throws IOException {
		iw.deleteDocuments(term);
	}
}
