package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class UniregTokenizer extends StandardTokenizer {

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

	public UniregTokenizer(Reader input) {
		super(input);
	}

	@Override
	public Token next(Token result) throws IOException {
		Token token = result;
		do {
			token = super.next(token);
		} while (token != null && isBlacklisted(token));
		return token;
	}

	private boolean isBlacklisted(Token token) {
		String str = new String(token.termBuffer(), 0, token.termLength());
		str = str.trim().toLowerCase();
		return blacklist.contains(str);
	}

}
