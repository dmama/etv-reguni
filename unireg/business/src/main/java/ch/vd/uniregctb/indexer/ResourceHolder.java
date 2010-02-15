package ch.vd.uniregctb.indexer;


public class ResourceHolder {
	private final LuceneSearcher indexReader;
	private final LuceneWriter indexWriter;

	public ResourceHolder(LuceneSearcher indexReader, LuceneWriter indexWriter) {
		this.indexReader = indexReader;
		this.indexWriter = indexWriter;
	}

	public LuceneSearcher getIndexReader() {
		return getIndexReader(true);
	}

	public LuceneSearcher getIndexReader(boolean throwException) {
		if( indexReader==null && throwException) {
			throw new RuntimeException("You can not used an IndexReader in this context.");
		}
		return indexReader;
	}

	public LuceneWriter getIndexWriter() {
		return getIndexWriter(true);
	}

	public LuceneWriter getIndexWriter(boolean throwException) {
		if( indexWriter==null && throwException ) {
			throw new RuntimeException("You can not used an IndexWriter in this context.");
		}
		return indexWriter;
	}

}