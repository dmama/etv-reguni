package ch.vd.uniregctb.indexer;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.util.Version;

public final class OurOwnStandardAnalyzer extends OurOwnAnalyzer {

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
			result = new ClassicTokenizer(Version.LUCENE_36, reader);
			//dumpTokenizer(result, reader);
			result = new ClassicFilter(result);
			//dumpTokenizer(result, reader);
			result = new ASCIIFoldingFilter(result);
			//dumpTokenizer(result, reader);
			result = new LowerCaseFilter(Version.LUCENE_36, result);
			//dumpTokenizer(result, reader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

}
