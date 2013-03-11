package ch.vd.uniregctb.indexer;

import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public abstract class OurOwnAnalyzer extends Analyzer {

	public static final Logger LOGGER = Logger.getLogger(OurOwnAnalyzer.class);

	@SuppressWarnings({"UnusedDeclaration"})
	protected void dumpTokenizer(TokenStream result, Reader reader) {

		if (result != null) {
			try {
				CharTermAttribute att = result.getAttribute(CharTermAttribute.class);
				LOGGER.debug("* " + result.getClass().getSimpleName());
				result.reset();
				while (result.incrementToken()) {
					LOGGER.debug(att.buffer());
				}
				reader.reset();
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e, e);
			}
		}
	}

}
