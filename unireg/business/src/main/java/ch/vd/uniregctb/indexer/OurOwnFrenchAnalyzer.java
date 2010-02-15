package ch.vd.uniregctb.indexer;

import java.io.Reader;

import org.apache.lucene.analysis.ISOLatin1AccentFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;

public class OurOwnFrenchAnalyzer extends OurOwnAnalyzer {

	//private Set stoptable = new HashSet();
	//private Set excltable = new HashSet();

	public OurOwnFrenchAnalyzer() {
		//stoptable = StopFilter.makeStopSet(FrenchAnalyzer.FRENCH_STOP_WORDS);
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
			//BlackListage de certains mots
			//result = new StandardTokenizer(reader);
			result = new UniregTokenizer(reader);
			//dumpTokenizer(result, reader);
			result = new StandardFilter(result);
			//dumpTokenizer(result, reader);
			//result = new StopFilter(result, stoptable);
			//dumpTokenizer(result, reader);
			result = new ISOLatin1AccentFilter(result);
			//dumpTokenizer(result, reader);
			// The FrenchStemFilter needs to get lower case letters to do its binz
			result = new LowerCaseFilter(result);
			//dumpTokenizer(result, reader);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

}
