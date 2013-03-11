package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.util.Version;

public final class OurOwnFrenchAnalyzer extends OurOwnAnalyzer {

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
			result = new ClassicTokenizer(Version.LUCENE_36, reader);
			result = new ClassicFilter(result);
			result = new ASCIIFoldingFilter(result);
			result = new LowerCaseFilter(Version.LUCENE_36, result);
			result = new UniregBlacklistFilter(result);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
		return super.reusableTokenStream(fieldName, reader);    //To change body of overridden methods use File | Settings | File Templates.
	}
}
