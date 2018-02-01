package ch.vd.unireg.indexer.jdbc;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.TopDocs;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.RequiresNewTransactionDefinition;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.IndexableData;
import ch.vd.unireg.indexer.SearchCallback;

import static org.junit.Assert.assertEquals;


public class IndexerTransactionalTest extends BusinessTest {

	private final Logger LOGGER = LoggerFactory.getLogger(IndexerTransactionalTest.class);

	private GlobalIndexInterface globalIndex;
	private static final String TYPE = "perfi";
	private final List<String> fields = new ArrayList<>();
	private int maxHits = 100;

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

		@Override
		public String getSubType() {
			return subType;
		}

		@Override
		public Document asDoc() {
			Document d = super.asDoc();

			d.add(new TextField("NOM", nom, Field.Store.YES));
			d.add(new TextField("PRENOM", prenom, Field.Store.YES));
			d.add(new TextField("DATE", date, Field.Store.YES));

			return d;
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		globalIndex = getBean(GlobalIndexInterface.class, "globalTiersIndex");
		globalIndex.overwriteIndex();
	}

	private void fillIndex() throws Exception {

		assertEquals(0, globalIndex.getApproxDocCount());

		doInNewTransaction(new TransactionCallback<Object>() {

			@Override
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
	@Transactional(rollbackFor = Throwable.class)
	public void testTransactionalRemove() throws Exception {

		fillIndex();

		// Search
		{
			assertHits(10, "PRENOM:Chri*");
		}

		doInNewTransaction(new TransactionCallback<Object>() {

			@Override
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
	@Transactional(rollbackFor = Throwable.class)
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
		globalIndex.search(query, maxHits, new SearchCallback() {
			@Override
			public void handle(TopDocs hits, DocGetter docGetter) throws Exception {
				assertEquals(count, hits.totalHits);
			}
		});
	}
}
