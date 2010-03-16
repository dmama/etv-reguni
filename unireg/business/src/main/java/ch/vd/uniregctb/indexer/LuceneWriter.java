package ch.vd.uniregctb.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.jdbc.JdbcStoreException;
import org.springframework.util.Assert;

/**
 * Classe principale d'indexation
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 *
 */
public class LuceneWriter extends LuceneEngine {

	private static final Logger LOGGER = Logger.getLogger(LuceneWriter.class);

	//private RegDate lastOptimize;

	/**
	 * IndexModifier
	 */
	private IndexWriter iw;

	/**
	 * Le Directory pour l'index (File ou JDBC)
	 */
	private final Directory directory;

	/**
	 * @param path
	 * @param creation
	 * @param deleteOldDocs
	 * @throws IndexerException
	 */
	public LuceneWriter(Directory directory, boolean createIt) throws IndexerException {
		this.directory = directory;

		Analyzer an = getFrenchAnalyzer();
		try {
			createIndexModifier(an, createIt);
		} catch (FileNotFoundException e) {
			try {
				createIndexModifier(an, true); // Force creation
			} catch (IOException ee) {
				throw new IndexerException(ee);
			}
		} catch (IOException e) {
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
		iw = new IndexWriter(directory, true/*autocommit*/, an, createIt);
		iw.setMaxBufferedDeleteTerms(1); // Force le delete a chaque fois
		Assert.isTrue(!createIt || iw.docCount() == 0, "L'indexeur n'est pas vide apres l'overwrite");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.vd.uniregctb.indexer.LuceneEngine#terminate()
	 */
	@Override
	public void close() throws IndexerException {

		if (iw != null) {
			try {
				iw.close();
			}
			catch (FileNotFoundException e) {
				// Nothing to do, the index is already empty
			}
			catch (JdbcStoreException e) {
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
		} catch (Exception e) {
			throw new IndexerException(e);
		}
		LOGGER.trace("END optimize()");
	}

	/**
	 * @throws IndexerException
	 */
	public void flush() throws IndexerException {
		LOGGER.trace("CALL flush()");
		try {
			iw.flush();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		LOGGER.trace("END flush()");
	}

	/**
	 * @return
	 */
	public int docCount() {
		return iw.docCount();
	}

	/**
	 * @param typeValue
	 * @param id
	 * @return
	 */
	private String generateDocumentID(String typeValue, long id) {
		String str = typeValue.toLowerCase() + "-" + id;
		return str;
	}

	/**
	 * Supprime un document
	 *
	 * @param id
	 * @param typeValue
	 * @return
	 * @throws IndexerException
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
	 * Cette methode ajoute les champs communs a tous les documents Lucene
	 *
	 * @param d le document Lucene
	 * @param id l'ID du document (ex: 54666)
	 * @param typeValue Le type du Document (ex: 'tiers')
	 * @param subTypeValue le sous type du Document (ex: 'habitant')
	 */
	private void addBaseFields(Document d, Long id, String typeValue, String subTypeValue) {

		String docId = generateDocumentID(typeValue, id);
		d.add(new Field(F_DOCID, docId, Field.Store.YES, Field.Index.UN_TOKENIZED));
		d.add(new Field(F_DOCTYPE, typeValue.toLowerCase(), Field.Store.YES, Field.Index.UN_TOKENIZED));
		d.add(new Field(F_DOCSUBTYPE, subTypeValue.toLowerCase(), Field.Store.YES, Field.Index.UN_TOKENIZED));
		d.add(new Field(F_ENTITYID, id.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED));
	}

	/**
	 * Methode principale d'indexation
	 *
	 * @param id
	 * @param typeValue
	 * @param subTypeValue
	 * @param onrp
	 * @param fields
	 * @throws IndexerException
	 */
	public void index(Long id, String typeValue, String subTypeValue, HashMap<String, String> listOfValues) throws IndexerException {

		Assert.notNull(iw);

		try {

			// Then create the new document
			Document d = new Document();
			addBaseFields(d, id, typeValue, subTypeValue);

			// Iterate on every document
			for (String k : listOfValues.keySet()) {

				Object v = listOfValues.get(k);

		        d.add(new Field(k,
		        		IndexerFormatHelper.objectToString(v),
		        		Field.Store.YES, Field.Index.TOKENIZED));
		        //LOGGER.debug(k + " = " + v);
		    }

			// Add to the index
			iw.addDocument(d);

			//LOGGER.info("Document "+id+" indexed.");

		} catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	public void deleteDocuments(Term term) throws IOException {
		iw.deleteDocuments(term);
	}

}
