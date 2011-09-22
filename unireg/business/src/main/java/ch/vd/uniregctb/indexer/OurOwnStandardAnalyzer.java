package ch.vd.uniregctb.indexer;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

public class OurOwnStandardAnalyzer extends OurOwnAnalyzer {

	public OurOwnStandardAnalyzer() {
	}

	@Override
	public final TokenStream tokenStream(String fieldName, Reader reader) {

		if (fieldName == null) {
			throw new IllegalArgumentException("fieldName must not be null");
		}
		if (reader == null) {
			throw new IllegalArgumentException("reader must not be null");
		}

		TokenStream result = null;
		try {
			// The real result
			result = new StandardTokenizer(Version.LUCENE_29, reader);
			//dumpTokenizer(result, reader);
			result = new StandardFilter(result);
			//dumpTokenizer(result, reader);
			result = new ASCIIFoldingFilter(result);
			//dumpTokenizer(result, reader);
			result = new LowerCaseFilter(result);
			//dumpTokenizer(result, reader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

}
