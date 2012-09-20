package ch.vd.uniregctb.indexer;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

public class OurOwnFrenchAnalyzer extends OurOwnAnalyzer {

	@Override
	public final TokenStream tokenStream(String fieldName, Reader reader) {

		if (fieldName == null) {
			throw new IllegalArgumentException("fieldName must not be null");
		}
		if (reader == null) {
			throw new IllegalArgumentException("reader must not be null");
		}

		TokenStream result;
		try {
			result = new StandardTokenizer(Version.LUCENE_29, reader);
			result = new StandardFilter(result);
			result = new ASCIIFoldingFilter(result);
			result = new LowerCaseFilter(result);
			result = new UniregBlacklistFilter(result);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

}
