package ch.vd.unireg.indexer;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.util.Version;

public final class OurOwnFrenchAnalyzer extends OurOwnAnalyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {

		final OurOwnReader effectiveReader = new OurOwnReader(reader);
		final Tokenizer source = new ClassicTokenizer(Version.LUCENE_41, effectiveReader);
		TokenStream result = new ClassicFilter(source);
		result = new ASCIIFoldingFilter(result);
		result = new LowerCaseFilter(Version.LUCENE_41, result);
		result = new UniregBlacklistFilter(result);

		return new TokenStreamComponents(source, result);
	}
}
