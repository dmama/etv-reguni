package ch.vd.uniregctb.indexer;

import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * Classe aidant a transformer les criteres de recherche en Lucene Query
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 *
 */
public abstract class LuceneEngine {

	//private static final Logger LOGGER = Logger.getLogger(LuceneEngine.class);

	/**
	 * Champ Lucene index nomme DOCTYPE
	 */
	public static final String F_DOCTYPE = "DOCTYPE";

	/**
	 * Champ Lucene index nomme DOCSUBTYPE
	 */
	public static final String F_DOCSUBTYPE = "DOCSUBTYPE";

	/**
	 * Champ Lucene index nomme DOCID
	 */
	public static final String F_DOCID = "DOCID";

	/**
	 * Champ Lucene index nomme ENTITYID
	 */
	public static final String F_ENTITYID = "ENTITYID";

	/**
	 * Constructeur
	 */
	public LuceneEngine() {
	}

	/**
	 * @throws IndexerException
	 */
	public void close() throws IndexerException {

	}

	/**
	 * @param value
	 * @return
	 * @throws IndexerException
	 */
	protected static TokenStream getFrenchTokenStream(String value) throws IndexerException {

		Analyzer an = getFrenchAnalyzer();
		return getTokenStream(value, an);
	}

	/**
	 * @param value
	 * @param an
	 * @return
	 * @throws IndexerException
	 */
	protected static TokenStream getTokenStream(String value, Analyzer an) throws IndexerException {

		TokenStream stream = null;
		try {
			StringReader reader = new StringReader(value);
			stream = an.tokenStream("", reader);
		} catch (Exception e) {
			throw new IndexerException(e);
		}
		return stream;
	}

	/**
	 * Retourne un Term qui contient les valeurs passees en parametre. Ne prend
	 * en compte que le premier "token" trouve.
	 *
	 * @param field
	 *            Le champ
	 * @param value
	 *            La valeur a matcher (le premier token est utilise seulement)
	 * @return Le term qui contient les le field/value
	 * @throws IndexerException
	 *             Pour toute erreur
	 */
	public static Term getTerm(String field, String value) throws IndexerException {

		final String token;
		try {
			final TokenStream stream = getFrenchTokenStream(value);
			stream.incrementToken();
			TermAttribute att = (TermAttribute) stream.getAttribute(TermAttribute.class);
			token = att.term();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		final Term term;
		if (token == null) {
			term = new Term(field, "");

		}
		else {
			term = new Term(field, token);
		}

		return term;
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type contient.
	 *
	 * @param field
	 * @param value
	 * @param minLength
	 *            la taille minimale des tokens (en caractères); ou <i>0</i> pour ne pas limiter la taille
	 * @return une BooleanQuery
	 * @throws IndexerException
	 */
	public static Query getTermsContient(String field, String value, int minLength) throws IndexerException {

		Query simpleQuery = null; // utilisé si un seul token
		BooleanQuery complexQuery = null; // utilisé si >1 token

		try {
			final TokenStream stream = getFrenchTokenStream(value);
			final TermAttribute att = (TermAttribute) stream.getAttribute(TermAttribute.class);
			
			while (stream.incrementToken()) {
				if (minLength == 0 || att.termLength() >= minLength) {
					final Query q = new WildcardQuery(newTermContient(field, att));
					if (complexQuery == null) {
						if (simpleQuery == null) {
							simpleQuery = q;
						}
						else {
							complexQuery = new BooleanQuery();
							complexQuery.add(simpleQuery, BooleanClause.Occur.MUST);
							complexQuery.add(q, BooleanClause.Occur.MUST);
							simpleQuery = null;
						}
					}
					else {
						complexQuery.add(q, BooleanClause.Occur.MUST);
					}
				}
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		return complexQuery == null ? simpleQuery : complexQuery;
	}

	/**
	 * Cree une BooleanQuery pour la recherche de type contenant
	 *
	 * @param field
	 * @param value
	 * @param minLength
	 *            la taille minimale des tokens (en caractères); ou <i>0</i> pour ne pas limiter la taille
	 * @return une BooleanQuery
	 * @throws IndexerException
	 */
	public static Query getTermsCommence(String field, String value, int minLength) throws IndexerException {

		Query simpleQuery = null; // utilisé si un seul token
		BooleanQuery complexQuery = null; // utilisé si >1 token

		try {
			final TokenStream stream = getFrenchTokenStream(value);
			final TermAttribute att = (TermAttribute) stream.getAttribute(TermAttribute.class);

			while (stream.incrementToken()) {
				if (minLength == 0 || att.termLength() >= minLength) {
				final Query q = new WildcardQuery(newTermCommence(field, att));
				if (complexQuery == null) {
					if (simpleQuery == null) {
						simpleQuery = q;
					}
					else {
						complexQuery = new BooleanQuery();
						complexQuery.add(simpleQuery, BooleanClause.Occur.MUST);
						complexQuery.add(q, BooleanClause.Occur.MUST);
						simpleQuery = null;
					}
				}
				else {
					complexQuery.add(q, BooleanClause.Occur.MUST);
				}
				}
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		return complexQuery == null ? simpleQuery : complexQuery;
	}

	/**
	 * Cree une BooleanQuery pour la recherche de type parfaite correspondance
	 *
	 * @param field
	 * @param value
	 * @return une BooleanQuery
	 * @throws IndexerException
	 */
	public static Query getTermsExact(String field, String value) throws IndexerException {

		Query simpleQuery = null; // utilisé si un seul token
		BooleanQuery complexQuery = null; // utilisé si >1 token

		try {
			final TokenStream stream = getFrenchTokenStream(value);
			final TermAttribute att = (TermAttribute) stream.getAttribute(TermAttribute.class);

			while (stream.incrementToken()) {
				final Query q = new TermQuery(newTerm(field, att));
				if (complexQuery == null) {
					if (simpleQuery == null) {
						simpleQuery = q;
					}
					else {
						complexQuery = new BooleanQuery();
						complexQuery.add(simpleQuery, BooleanClause.Occur.MUST);
						complexQuery.add(q, BooleanClause.Occur.MUST);
						simpleQuery = null;
					}
				}
				else {
					complexQuery.add(q, BooleanClause.Occur.MUST);
				}
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		return complexQuery == null ? simpleQuery : complexQuery;
	}

	private static Term newTerm(String field, TermAttribute attribute) {
		return new Term(field, attribute.term());
	}

	private static Term newTermCommence(String field, TermAttribute attribute) {
		StringBuilder txt = new StringBuilder(attribute.termLength() + 1);
		txt.append(attribute.termBuffer(), 0, attribute.termLength());
		txt.append('*');
		return new Term(field, txt.toString());
	}

	private static Term newTermContient(String field, TermAttribute attribute) {
		StringBuilder txt = new StringBuilder(attribute.termLength() + 2);
		txt.append('*');
		txt.append(attribute.termBuffer(), 0, attribute.termLength());
		txt.append('*');
		return new Term(field, txt.toString());
	}

	/**
	 * Cree une BooleanQuery pour la recherche de type recherche floue
	 *
	 * @param field
	 * @param value
	 * @return une BooleanQuery
	 * @throws IndexerException
	 */
	public static BooleanQuery getTermsFuzzy(String field, String value) throws IndexerException {

		// Exact search
		BooleanQuery booleanQuery = new BooleanQuery();
		try {
			// Use teh standard analyzer.
			// We don't want the tokens to be too much changed by the Analyzer
			// in Fuzzy
			TokenStream stream = getTokenStream(value, getStandardAnalyzer());
			final TermAttribute att = (TermAttribute) stream.getAttribute(TermAttribute.class);

			while (stream.incrementToken()) {
				Query query = new FuzzyQuery(newTerm(field, att));
				booleanQuery.add(query, BooleanClause.Occur.MUST);
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		return booleanQuery;
	}

	/**
	 * @return
	 */
	protected static Analyzer getFrenchAnalyzer() {
		// return new StandardAnalyzer();
		// return new SnowballAnalyzer("French",
		// FrenchAnalyzer.FRENCH_STOP_WORDS),
		// return new FrenchAnalyzer();
		return new OurOwnFrenchAnalyzer();
	}

	/**
	 * @return
	 */
	protected static Analyzer getStandardAnalyzer() {
		return new OurOwnStandardAnalyzer();
	}
}
