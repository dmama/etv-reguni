package ch.vd.uniregctb.indexer;

import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import ch.vd.registre.base.utils.Assert;

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

		final Token token;
		try {
			final TokenStream stream = getFrenchTokenStream(value);
			token = stream.next(new Token());
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		final Term term;
		if (token == null) {
			term = new Term(field, "");

		}
		else {
			term = newTerm(field, token);
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
			Token token = stream.next(new Token());
			while (token != null) {
				if (minLength == 0 || token.termLength() >= minLength) {
					final Query q = new WildcardQuery(newTermContient(field, token));
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
				token.clear();
				token = stream.next(token);
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		Assert.isTrue(simpleQuery != null || complexQuery != null);
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
			Token token = stream.next(new Token());
			while (token != null) {
				if (minLength == 0 || token.termLength() >= minLength) {
					final Query q = new WildcardQuery(newTermCommence(field, token));
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
				token.clear();
				token = stream.next(token);
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		Assert.isTrue(simpleQuery != null || complexQuery != null);
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
			Token token = stream.next(new Token());
			while (token != null) {
				final Query q = new TermQuery(newTerm(field, token));
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
				token.clear();
				token = stream.next(token);
			}
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		Assert.isTrue(simpleQuery != null || complexQuery != null);
		return complexQuery == null ? simpleQuery : complexQuery;
	}

	private static Term newTerm(String field, Token token) {
		final String txt = new String(token.termBuffer(), 0, token.termLength());
		return new Term(field, txt);
	}

	private static Term newTermCommence(String field, Token token) {
		StringBuilder txt = new StringBuilder(token.termLength() + 1);
		txt.append(token.termBuffer(), 0, token.termLength());
		txt.append('*');
		return new Term(field, txt.toString());
	}

	private static Term newTermContient(String field, Token token) {
		StringBuilder txt = new StringBuilder(token.termLength() + 2);
		txt.append('*');
		txt.append(token.termBuffer(), 0, token.termLength());
		txt.append('*');
		return new Term(field, txt.toString());
	}

	/**
	 * Cree une BooleanQuery pour la recherche de type phonetique
	 *
	 * @param field
	 * @param value
	 * @return une BooleanQuery
	 * @throws IndexerException
	 */
	public static BooleanQuery getTermsPhonetique(String field, String value) throws IndexerException {

		// Phonetique
		BooleanQuery booleanQuery = new BooleanQuery();
		Query q = new PhonetixQuery(new Term(field, value));
		booleanQuery.add(q, BooleanClause.Occur.MUST);
		return booleanQuery;
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
			Token token = stream.next(new Token());
			while (token != null) {
				Query query = new FuzzyQuery(newTerm(field, token));
				booleanQuery.add(query, BooleanClause.Occur.MUST);

				token.clear();
				token = stream.next(token);
			}
		} catch (Exception e) {
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
