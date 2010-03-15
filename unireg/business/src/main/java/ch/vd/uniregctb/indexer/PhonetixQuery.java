package ch.vd.uniregctb.indexer;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;

public class PhonetixQuery extends MultiTermQuery {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3978194096218454624L;

	public PhonetixQuery(Term term) {
		super(term);
	}

	protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
		return new PhonetixTermEnum(reader, getTerm());
	}

	public boolean equals(Object o) {
		if (o instanceof PhonetixQuery)
			return super.equals(o);

		return false;
	}

	public Query rewrite(IndexReader reader) throws IOException {
		return super.rewrite(reader);
	}
}
