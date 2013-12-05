package ch.vd.uniregctb.lucene;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.uniregctb.indexer.GlobalIndex;
import ch.vd.uniregctb.indexer.SearchAllCallback;
import ch.vd.uniregctb.indexer.lucene.FSIndexProvider;
import ch.vd.uniregctb.indexer.lucene.IndexProvider;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;

public class LuceneExtractor {

	private static final String LuceneDirectory = "/tmp/lucene";

	public static void main(String[] args) throws Exception {
		final List<String> res = new LinkedList<>();
		final IndexProvider provider = new FSIndexProvider(LuceneDirectory);
		final GlobalIndex index = new GlobalIndex(provider);
		index.afterPropertiesSet();
		try {
			final Set<String> docTypes = new HashSet<>(Arrays.asList("nonhabitant", "habitant"));
			final Query query = new MatchAllDocsQuery();
			index.searchAll(query, new SearchAllCallback() {
				@Override
				public void handle(int doc, DocGetter docGetter) throws Exception {
					final Document document = docGetter.get(doc);
					final String docsubtype = document.getField("DOCSUBTYPE").stringValue();
					if (docTypes.contains(docsubtype)) {
						final String entityId = document.getField(LuceneHelper.F_ENTITYID).stringValue();
						final String nom = document.getField("D_NOM1").stringValue();
						res.add(entityId + ";" + nom);
					}
				}
			});
		}
		finally {
			index.destroy();
		}

		try (FileWriter writer = new FileWriter(LuceneDirectory + ".csv")) {
			for (String line : res) {
				writer.write(line);
				writer.write('\n');
			}
		}
	}

}
