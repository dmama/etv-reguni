package ch.vd.unireg.indexer;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.util.Version;

public final class OurOwnStandardAnalyzer extends OurOwnAnalyzer {

	public OurOwnStandardAnalyzer() {
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {

		final Tokenizer source = new ClassicTokenizer(Version.LUCENE_41, reader);
		TokenStream result = new ClassicFilter(source);
		result = new ASCIIFoldingFilter(result);
		result = new LowerCaseFilter(Version.LUCENE_41, result);

		return new TokenStreamComponents(source, result);
	}
}
