package ch.vd.uniregctb.indexer;

import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

public abstract class OurOwnAnalyzer extends Analyzer {

	public static final Logger LOGGER = Logger.getLogger(OurOwnAnalyzer.class);

	protected void dumpTokenizer(TokenStream result, Reader reader) {

		if (result != null) {
			try {
				Token token;
				LOGGER.debug("* " + result.getClass().getSimpleName());
				while ((token = result.next()) != null) {
					LOGGER.debug(token);
				}
				reader.reset();
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.error(e, e);
			}
		}
	}

}
