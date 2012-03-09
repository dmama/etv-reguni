package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class UniregBlacklistFilter extends TokenFilter {

	private final static Set<String> blacklist = new HashSet<String>();

	static {
		blacklist.add("succ");
		blacklist.add("succession");
		blacklist.add("née");
		blacklist.add("mme");
		blacklist.add("def");
		blacklist.add("déf");
		blacklist.add("défunt");
		blacklist.add("défunte");
		blacklist.add("rue");
		blacklist.add("rte");
		blacklist.add("pa.mme");
		blacklist.add("pa.m");
	}

	private final TermAttribute att;

	protected UniregBlacklistFilter(TokenStream input) {
		super(input);
		att = getAttribute(TermAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		boolean res = input.incrementToken();
		while (res && isBlackListed(att)) {
			res = input.incrementToken();
		}
		return res;
	}

	private static boolean isBlackListed(TermAttribute att) {
		if (att.termLength() == 0) {
			return false;
		}
		final char first = att.termBuffer()[0];
		return (first == 's' || first == 'n' || first == 'd' || first == 'r' || first == 'p') // on essaie à tout prix de ne pas appeler att.term() qui provoque l'instanciation d'une string 
				&& blacklist.contains(att.term());
	}
}
