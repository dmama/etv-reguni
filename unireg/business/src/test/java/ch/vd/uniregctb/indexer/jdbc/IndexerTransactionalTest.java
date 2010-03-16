package ch.vd.uniregctb.indexer.jdbc;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Hits;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.RequiresNewTransactionDefinition;
import ch.vd.uniregctb.indexer.GenericIndexable;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.LuceneEngine;
import ch.vd.uniregctb.indexer.SearchCallback;


public class IndexerTransactionalTest extends BusinessTest {

	private final Logger LOGGER = Logger.getLogger(IndexerTransactionalTest.class);

	private GlobalIndexInterface globalIndex;
	private static final String TYPE = "perfi";
	private final List<String> fields = new ArrayList<String>();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		globalIndex = getBean(GlobalIndexInterface.class, "globalIndex");
		globalIndex.overwriteIndex();

		fields.add("NOM");
		fields.add("PRENOM");
		fields.add("DATE");
	}

	private void fillIndex() throws Exception {

		assertEquals(0, globalIndex.getApproxDocCount());

		doInNewTransaction(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				// Ajout des entités
				{

					int nbDocs = 10;
					LOGGER.info("Begin indexing of "+nbDocs+" documents");

					List<String> values;
					for (int i=0;i<nbDocs;i++) {
						values = new ArrayList<String>();
						values.add("Duchmol-"+i);
						values.add("Christian-"+i);
						values.add("200801"+i);
						GenericIndexable indexable = new GenericIndexable(i, TYPE, fields, values);
						globalIndex.indexEntity(new IndexableData(indexable));

						if (i % 1000 == 0) {
							LOGGER.info("Indexation: "+i);
						}
					}
				}
				return null;
			}

		});
		assertEquals(10, globalIndex.getExactDocCount());

		globalIndex.flush();
		assertEquals(10, globalIndex.getExactDocCount());
	}

	@Test
	public void testTransactionalRemove() throws Exception {

		fillIndex();

		// Search
		{
			assertHits(10, "PRENOM:Chri*");
		}

		doInNewTransaction(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				// Remplace une entité
				{
					ArrayList<String> values = new ArrayList<String>();
					values.add("Blanc");
					values.add("Marcel");
					values.add("20071212");

					GenericIndexable indexable = new GenericIndexable(5, TYPE, fields, values);
					globalIndex.removeThenIndexEntity(new IndexableData(indexable));
				}

				// On doit en avoir une de moins
				{
					assertHits(9, "PRENOM:Chri*");
				}

				// On doit trouver la nouvelle
				{
					assertHits(1, "PRENOM:Marcel");
				}
				return null;
			}

		});

		// Apres le commit, idem
		{
			assertHits(9, "PRENOM:Chri*");
		}

		// Apres le commit, idem
		{
			assertHits(1, "PRENOM:Marcel");
		}
	}

	@Ignore
	@Test
	// ce test est ignoré parce qu'il ne peut marcher que si l'indexer est transactionnel donc JDBC
	public void _testTransactionalRollback() throws Exception {

		fillIndex();

		// Fais une recherche
		{
			assertHits(10, "PRENOM:Chri*");
		}

		TransactionStatus tx = transactionManager.getTransaction(new RequiresNewTransactionDefinition());
		try {

			// Remplace une entité
			{
				ArrayList<String> values = new ArrayList<String>();
				values.add("Blanc");
				values.add("Marcel");
				values.add("20071212");

				GenericIndexable indexable = new GenericIndexable(5, TYPE, fields, values);
				globalIndex.removeThenIndexEntity(new IndexableData(indexable));
			}

			// Après remplacement, on doit en trouver moins
			{
				assertHits(9, "PRENOM:Chri*");
			}
		}
		finally {
			transactionManager.rollback(tx);
		}
		globalIndex.flush();

		// Apres Rollback on doit en trouver 10 de nouveau
		{
			assertHits(10, "PRENOM:Chri*");
		}
	}

	private void assertHits(final int count, String query) {
		globalIndex.search(query, new SearchCallback() {
			public void handle(Hits hits) throws Exception {
				LuceneEngine.debugHits(hits);
				assertEquals(count, hits.length());
			}
		});
	}
}
