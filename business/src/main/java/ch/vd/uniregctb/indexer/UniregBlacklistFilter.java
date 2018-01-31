package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public final class UniregBlacklistFilter extends TokenFilter {

	private static final Set<String> blacklist = new HashSet<>();

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

	private final CharTermAttribute att;

	protected UniregBlacklistFilter(TokenStream input) {
		super(input);
		att = getAttribute(CharTermAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		boolean res = input.incrementToken();
		while (res && isBlackListed(att)) {
			res = input.incrementToken();
		}
		return res;
	}

	private static boolean isBlackListed(CharTermAttribute att) {
		if (att.length() == 0) {
			return false;
		}
		final char first = att.buffer()[0];
		return (first == 's' || first == 'm' || first == 'n' || first == 'd' || first == 'r' || first == 'p')
				// on essaie à tout prix de ne pas appeler att.term() qui provoque l'instanciation d'une string
				&& blacklist.contains(new String(att.buffer(), 0, att.length()));
	}
}
