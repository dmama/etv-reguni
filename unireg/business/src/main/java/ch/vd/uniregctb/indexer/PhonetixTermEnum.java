package ch.vd.uniregctb.indexer;

import java.io.IOException;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;

public class PhonetixTermEnum extends FilteredTermEnum {

	Term searchTerm;
	String field = "";
	String text = "";
	boolean endEnum = false;

	/**
	 * Creates a new <code>PhonetixTermEnum</code>.  Passing in a
	 * {@link org.apache.lucene.index.Term Term} that does not contain a
	 * <code>WILDCARD_CHAR</code> will cause an exception to be thrown.
	 * <p>
	 * After calling the constructor the enumeration is already pointing to the first 
	 * valid term if such a term exists.
	 */
	public PhonetixTermEnum(IndexReader reader, Term term) throws IOException {
		super();
		searchTerm = term;
		field = searchTerm.field();
		text = searchTerm.text();

		setEnum(reader.terms(new Term(searchTerm.field(), text)));
	}

	protected final boolean termCompare(Term term) {
		if (field.equals(term.field())) {
			String searchText = term.text();
			boolean result = new DoubleMetaphone().isDoubleMetaphoneEqual(searchText, text);
			if (result) {
				searchText = null; // For BP
			}
			return result;
		}
		endEnum = true;
		return false;
	}

	public final float difference() {
		return 1.0f;
	}

	public final boolean endEnum() {
		return endEnum;
	}

	/********************************************
	 * String equality with support for wildcards
	 ********************************************/

	public void close() throws IOException
	{
		super.close();
		searchTerm = null;
		field = null;
		text = null;
	}

}
