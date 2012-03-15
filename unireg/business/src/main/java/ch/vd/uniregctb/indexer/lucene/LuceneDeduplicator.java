package ch.vd.uniregctb.indexer.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;

import ch.vd.uniregctb.indexer.IndexerException;

public class LuceneDeduplicator {

	private static final Logger LOGGER = Logger.getLogger(LuceneDeduplicator.class);

	private Directory directory;

	public LuceneDeduplicator(Directory directory) {
		this.directory = directory;
	}

	/**
	 * Recherche, compte et supprime les doublons des documents stockés dans un index lucène.
	 * <p/>
	 * <b>Note:</b> il est de la responsabilité de l'appelant de s'assurer que
	 * <ul>
	 *     <li>l'index lucène n'est pas locké, et que </li>
	 *     <li>nul autre thread n'accède à l'index pendant la durée de l'exécution de cette méthode.</li>
	 * </ul>
	 *
	 * @return le nombre de documents supprimés
	 * @throws IOException en cas d'erreur d'accès à l'index lucène sur le disque
	 */
	public int clean() throws IOException {
		IndexReader reader = null;
		try {
			reader = IndexReader.open(directory, false);

			final Set<Term> duplicatedTerms = new HashSet<Term>();

			// on boucle sur tous les valeurs du champ ENTITYID
			final TermEnum e = reader.terms(new Term(LuceneHelper.F_ENTITYID));
			while (e.term() != null && LuceneHelper.F_ENTITYID.equals(e.term().field())) {
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
		finally {
			safeClose(reader);
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
		buffer.append('{');

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

			buffer.append(name).append(":\"").append(value).append('\"');

			if (i != fields.size() - 1) {
				buffer.append(", ");
			}
		}

		buffer.append('}');
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
}
