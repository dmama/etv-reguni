package ch.vd.uniregctb.indexer.jdbc;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.RequiresNewTransactionDefinition;
import ch.vd.uniregctb.indexer.*;
import ch.vd.uniregctb.indexer.DocHit;
import static junit.framework.Assert.assertEquals;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.List;


public class IndexerTransactionalTest extends BusinessTest {

	private final Logger LOGGER = Logger.getLogger(IndexerTransactionalTest.class);

	private GlobalIndexInterface globalIndex;
	private static final String TYPE = "perfi";
	private final List<String> fields = new ArrayList<String>();

	private static class Data extends IndexableData {

		private String nom;
		private String prenom;
		private String date;

		private Data(Long id, String type, String subType, String nom, String prenom, String date) {
			super(id, type, subType);
			this.nom = nom;
			this.prenom = prenom;
			this.date = date;
		}

		public String getSubType() {
			return subType;
		}

		@Override
		public Document asDoc() {
			Document d = super.asDoc();

			d.add(new Field("NOM", nom, Field.Store.YES, Field.Index.ANALYZED));
			d.add(new Field("PRENOM", prenom, Field.Store.YES, Field.Index.ANALYZED));
			d.add(new Field("DATE", date, Field.Store.YES, Field.Index.ANALYZED));

			return d;
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		globalIndex = getBean(GlobalIndexInterface.class, "globalIndex");
		globalIndex.overwriteIndex();
	}

	private void fillIndex() throws Exception {

		assertEquals(0, globalIndex.getApproxDocCount());

		doInNewTransaction(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				// Ajout des entités
				{

					int nbDocs = 10;
					LOGGER.info("Begin indexing of "+nbDocs+" documents");

					for (int i=0;i<nbDocs;i++) {

						Data d = new Data((long) i, TYPE, null, "Duchmol-" + i, "Christian-" + i, "200801" + i);
						globalIndex.indexEntity(d);

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
					Data d = new Data(5L, TYPE, null, "Blanc", "Marcel", "20071212");
					globalIndex.removeThenIndexEntity(d);
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
				Data d = new Data(5L, TYPE, null, "Blanc", "Marcel", "20071212");
				globalIndex.removeThenIndexEntity(d);
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
			public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
				assertEquals(count, hits.size());
			}
		});
	}
}
