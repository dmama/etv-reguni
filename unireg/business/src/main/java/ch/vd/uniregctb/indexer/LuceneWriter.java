package ch.vd.uniregctb.indexer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
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
		openWriter(createIt);
	}

	private void openWriter(boolean createIt) {
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

	public int deleteDuplicate() throws IndexerException {

		IndexReader reader = null;
		try {
			close(); // on ferme temporairement le writer car il prend un lock exclusif sur l'index et pour rechercher/supprimer les doublons ont doit passer par un reader
			reader = IndexReader.open(directory, false);

			final Set<Term> duplicatedTerms = new HashSet<Term>();

			// on boucle sur tous les valeurs du champ ENTITYID
			final TermEnum e = reader.terms(new Term(LuceneEngine.F_ENTITYID));
			while (e.term() != null && LuceneEngine.F_ENTITYID.equals(e.term().field())) {
				if (e.docFreq() > 1) { // attention, un document effacé est aussi comptabilisé !
					duplicatedTerms.add(e.term());
				}
				if (!e.next()) {
					break;
				}
			}

			// on supprime les doublons
			int deleteDocs = 0;
			for (Term t : duplicatedTerms) {
				deleteDocs += deleteDuplicatedDocs(t, reader);
			}

			return deleteDocs;
		}
		catch (IOException e) {
			throw new IndexerException(e);
		}
		finally {
			safeClose(reader);
			openWriter(false); // on ré-ouvre le writer pour rétablir la situation normale
		}
	}

	/**
	 * Détecte et supprime les documents dupliqués.
	 *
	 * @param term   le term sur l'entity id auquel est associé plusieurs (= doublons) documents
	 * @param reader l'indexe reader
	 * @return le nombre de documents supprimés
	 * @throws IOException en cas d'erreur dans la manipulation de l'index
	 */
	private int deleteDuplicatedDocs(Term term, IndexReader reader) throws IOException {

		final List<Integer> docNums;

		// recherche tous les documents ayant le term spécifié
		IndexSearcher is = null;
		try {
			is = new IndexSearcher(reader);

			final CollectAll collector = new CollectAll();
			is.search(new TermQuery(term), collector);
			docNums = collector.getAllDocs();

			if (docNums.size() < 2) {
				// dans le cas d'un index non-optimisé, il est possible que le terme courant possède un ou plusieurs documents effacés : ces documents n'apparaissent plus
				// lors d'une recherche, mais sont encore comptabilités dans les statistiques des terms. On peut donc ignorer silencieusement ces cas.
				return 0;
			}

			if (LOGGER.isDebugEnabled()) {
				final StringBuilder s = new StringBuilder();
				s.append("Les documents suivants sont dupliqués :");
				for (Integer docNum : docNums) {
					final Document doc = is.doc(docNum);
					s.append("\n - ").append(print(doc));
				}
				LOGGER.debug(s);
			}
		}
		finally {
			safeClose(is);
		}

		// supprime tous les documents sauf le dernier
		for (int i = docNums.size() - 2; i >= 0; i--) {
			final Integer docNum = docNums.get(i);
			LOGGER.warn("Suppression du doublon (doc #" + docNum + ") pour le term \"" + term.toString() + "\".");
			reader.deleteDocument(docNum);
		}

		return docNums.size() - 1;
	}

	private static String print(Document doc) {

		final StringBuilder buffer = new StringBuilder();
		buffer.append("{");

		final List<Fieldable> fields = doc.getFields();
		for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {

			final Fieldable field = fields.get(i);
			final String name = field.name();
			String value = doc.get(name);

			if ("D_INDEXATION_DATE".equals(name)) { // petit exception pour afficher ce champ technique de manière lisible
				final long millisecondes = Long.parseLong(value);
				final Date d = new Date(millisecondes);
				value = d.toString();
			}

			buffer.append(name).append(":\"").append(value).append("\"");

			if (i != fields.size() - 1) {
				buffer.append(", ");
			}
		}

		buffer.append("}");
		return buffer.toString();
	}

	private static class CollectAll extends Collector {

		private List<Integer> allDocs = new ArrayList<Integer>();
		private int docBase;

		@Override
		public void setScorer(Scorer scorer) throws IOException {
		}

		@Override
		public void collect(int doc) throws IOException {
			allDocs.add(docBase + doc);
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase) throws IOException {
			this.docBase = docBase;
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return false;
		}

		public List<Integer> getAllDocs() {
			return allDocs;
		}
	}

	private static void safeClose(IndexSearcher is) {
		try {
			if (is != null) {
				is.close();
			}
		}
		catch (IOException e) {
			throw new IndexerException(e);
		}
	}

	private static void safeClose(IndexReader directoryReader) {
		try {
			if (directoryReader != null) {
				directoryReader.close();
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
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
