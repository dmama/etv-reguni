package ch.vd.unireg.indexer.lucene;

import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.simpleindexer.LuceneData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.OurOwnFrenchAnalyzer;
import ch.vd.unireg.indexer.OurOwnStandardAnalyzer;

/**
 * Classe aidant a transformer les criteres de recherche en Lucene Query
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 */
public abstract class LuceneHelper {

	public static final String F_DOCTYPE = "DOCTYPE";
	public static final String F_DOCSUBTYPE = "DOCSUBTYPE";
	public static final String F_DOCID = "DOCID";
	public static final String F_ENTITYID = LuceneData.F_ID;

	private LuceneHelper() {
	}

	private static TokenStream getFrenchTokenStream(String value) throws IndexerException {

		Analyzer an = getFrenchAnalyzer();
		return getTokenStream(value, an);
	}

	private static TokenStream getTokenStream(String value, Analyzer an) throws IndexerException {

		TokenStream stream;
		try {
			StringReader reader = new StringReader(value);
			stream = an.tokenStream("", reader);
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		return stream;
	}

	/**
	 * Retourne un Term qui contient les valeurs passees en parametre. Ne prend en compte que le premier "token" trouve.
	 *
	 * @param field Le champ
	 * @param value La valeur a matcher (le premier token est utilise seulement)
	 * @return Le term qui contient les le field/value
	 * @throws IndexerException Pour toute erreur
	 */
	public static Term getTerm(String field, String value) throws IndexerException {

		final Term term;

		try (final TokenStream stream = getFrenchTokenStream(value)) {
			stream.reset();
			stream.incrementToken();

			final CharTermAttribute token = stream.getAttribute(CharTermAttribute.class);
			if (token == null || token.length() == 0) {
				term = new Term(field, "");
			}
			else {
				term = newTerm(field, token);
			}

			stream.end();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		return term;
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type contient sur <b>tous</b> les termes.
	 *
	 * @param field     le champ lucene sur lequel s'exécutera la query
	 * @param value     le ou les critères de recherche
	 * @param minLength la taille minimale des tokens (en caractères); ou <i>0</i> pour ne pas limiter la taille
	 * @return la query résultante
	 * @throws IndexerException en cas de problème dans l'indexer
	 */
	public static Query getTermsContient(String field, String value, int minLength) throws IndexerException {
		return getTermsContient_(field, value, minLength, BooleanClause.Occur.MUST);
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type contient sur <b>n'importe lequel</b> les termes.
	 *
	 * @param field     le champ lucene sur lequel s'exécutera la query
	 * @param value     le ou les critères de recherche
	 * @param minLength la taille minimale des tokens (en caractères); ou <i>0</i> pour ne pas limiter la taille
	 * @return la query résultante
	 * @throws IndexerException en cas de problème dans l'indexer
	 */
	public static Query getAnyTermsContient(String field, String value, int minLength) throws IndexerException {
		return getTermsContient_(field, value, minLength, BooleanClause.Occur.SHOULD);
	}

	private static Query getTermsContient_(String field, String value, int minLength, BooleanClause.Occur occur) {

		try (final TokenStream stream = getFrenchTokenStream(value)) {
			final CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
			Query simpleQuery = null; // utilisé si un seul token
			BooleanQuery complexQuery = null; // utilisé si >1 token

			stream.reset();
			while (stream.incrementToken()) {
				if (minLength == 0 || att.length() >= minLength) {
					final Query q = new WildcardQuery(newTermContient(field, att));
					if (complexQuery == null) {
						if (simpleQuery == null) {
							simpleQuery = q;
						}
						else {
							complexQuery = new BooleanQuery();
							complexQuery.add(simpleQuery, occur);
							complexQuery.add(q, occur);
							simpleQuery = null;
						}
					}
					else {
						complexQuery.add(q, occur);
					}
				}
			}
			stream.end();

			// [UNIREG-2715] si value.length() est plus petit que minLength, on doit retourner null : Assert.isTrue(simpleQuery != null || complexQuery != null);
			return complexQuery == null ? simpleQuery : complexQuery;
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type contenant sur <b>tous</b> les termes.
	 *
	 * @param field     le champ lucene sur lequel s'exécutera la query
	 * @param value     le ou les critères de recherche
	 * @param minLength la taille minimale des tokens (en caractères); ou <i>0</i> pour ne pas limiter la taille
	 * @return la query résultante
	 * @throws IndexerException en cas de problème dans l'indexer
	 */
	@Nullable(value = "Si la valeur, bien que non-vide, ne contient aucun token (suite de caractères spéciaux...)")
	public static Query getTermsCommence(String field, String value, int minLength) throws IndexerException {
		return getTermsCommence(field, value, minLength, BooleanClause.Occur.MUST);
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type contenant sur <b>n'importe lequel</b> les termes.
	 *
	 * @param field     le champ lucene sur lequel s'exécutera la query
	 * @param value     le ou les critères de recherche
	 * @param minLength la taille minimale des tokens (en caractères); ou <i>0</i> pour ne pas limiter la taille
	 * @return la query résultante
	 * @throws IndexerException en cas de problème dans l'indexer
	 */
	@Nullable(value = "Si la valeur, bien que non-vide, ne contient aucun token (suite de caractères spéciaux...)")
	public static Query getAnyTermsCommence(String field, String value, int minLength) throws IndexerException {
		return getTermsCommence(field, value, minLength, BooleanClause.Occur.SHOULD);
	}

	@Nullable(value = "Si la valeur, bien que non-vide, ne contient aucun token (suite de caractères spéciaux...)")
	private static Query getTermsCommence(String field, String value, int minLength, BooleanClause.Occur occur) {

		try (final TokenStream stream = getFrenchTokenStream(value)) {
			final CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
			Query simpleQuery = null; // utilisé si un seul token
			BooleanQuery complexQuery = null; // utilisé si >1 token

			stream.reset();
			while (stream.incrementToken()) {
				if (minLength == 0 || att.length() >= minLength) {
					final Query q = new WildcardQuery(newTermCommence(field, att));
					if (complexQuery == null) {
						if (simpleQuery == null) {
							simpleQuery = q;
						}
						else {
							complexQuery = new BooleanQuery();
							complexQuery.add(simpleQuery, occur);
							complexQuery.add(q, occur);
							simpleQuery = null;
						}
					}
					else {
						complexQuery.add(q, occur);
					}
				}
			}
			stream.end();

			return complexQuery == null ? simpleQuery : complexQuery;
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type parfaite correspondance sur <b>tous</b> les termes.
	 *
	 * @param field le champ lucene sur lequel s'exécutera la query
	 * @param value le ou les critères de recherche
	 * @return la query résultante
	 * @throws IndexerException en cas de problème dans l'indexer
	 */
	@Nullable(value = "Si la valeur, bien que non-vide, ne contient aucun token (suite de caractères spéciaux...)")
	public static Query getTermsExact(String field, String value) throws IndexerException {
		return getTermsExact(field, value, BooleanClause.Occur.MUST);
	}

	/**
	 * Crée une BooleanQuery pour la recherche de type parfaite correspondance sur <b>n'importe lequel</b> des termes.
	 *
	 * @param field le champ lucene sur lequel s'exécutera la query
	 * @param value le ou les critères de recherche
	 * @return la query résultante
	 * @throws IndexerException en cas de problème dans l'indexer
	 */
	@Nullable(value = "Si la valeur, bien que non-vide, ne contient aucun token (suite de caractères spéciaux...)")
	public static Query getAnyTermsExact(String field, String value) throws IndexerException {
		return getTermsExact(field, value, BooleanClause.Occur.SHOULD);
	}

	@Nullable(value = "Si la valeur, bien que non-vide, ne contient aucun token (suite de caractères spéciaux...)")
	private static Query getTermsExact(String field, String value, BooleanClause.Occur occur) {

		try (final TokenStream stream = getFrenchTokenStream(value)) {
			final CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);
			Query simpleQuery = null; // utilisé si un seul token
			BooleanQuery complexQuery = null; // utilisé si >1 token

			stream.reset();
			while (stream.incrementToken()) {
				final Query q = new TermQuery(newTerm(field, att));
				if (complexQuery == null) {
					if (simpleQuery == null) {
						simpleQuery = q;
					}
					else {
						complexQuery = new BooleanQuery();
						complexQuery.add(simpleQuery, occur);
						complexQuery.add(q, occur);
						simpleQuery = null;
					}
				}
				else {
					complexQuery.add(q, occur);
				}
			}
			stream.end();

			return complexQuery == null ? simpleQuery : complexQuery;
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
	}

	private static Term newTerm(String field, CharTermAttribute attribute) {
		return new Term(field, attribute.toString());
	}

	private static Term newTermCommence(String field, CharTermAttribute attribute) {
		StringBuilder txt = new StringBuilder(attribute.length() + 1);
		txt.append(attribute.buffer(), 0, attribute.length());
		txt.append('*');
		return new Term(field, txt.toString());
	}

	private static Term newTermContient(String field, CharTermAttribute attribute) {
		StringBuilder txt = new StringBuilder(attribute.length() + 2);
		txt.append('*');
		txt.append(attribute.buffer(), 0, attribute.length());
		txt.append('*');
		return new Term(field, txt.toString());
	}

	/**
	 * Cree une BooleanQuery pour la recherche de type recherche floue
	 *
	 * @return une BooleanQuery
	 * @throws IndexerException
	 */
	public static BooleanQuery getTermsFuzzy(String field, String value) throws IndexerException {

		final BooleanQuery booleanQuery = new BooleanQuery();
		// Use teh standard analyzer.
		// We don't want the tokens to be too much changed by the Analyzer in Fuzzy
		try (final TokenStream stream = getTokenStream(value, getStandardAnalyzer())) {
			final CharTermAttribute att = stream.getAttribute(CharTermAttribute.class);

			stream.reset();
			while (stream.incrementToken()) {
				final Query query = new FuzzyQuery(newTerm(field, att));
				booleanQuery.add(query, BooleanClause.Occur.MUST);
			}
			stream.end();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		return booleanQuery.clauses() != null && !booleanQuery.clauses().isEmpty() ? booleanQuery : null;
	}

	private static Analyzer getFrenchAnalyzer() {
		// return new StandardAnalyzer();
		// return new SnowballAnalyzer("French",
		// FrenchAnalyzer.FRENCH_STOP_WORDS),
		// return new FrenchAnalyzer();
		return new OurOwnFrenchAnalyzer();
	}

	private static Analyzer getStandardAnalyzer() {
		return new OurOwnStandardAnalyzer();
	}
}
