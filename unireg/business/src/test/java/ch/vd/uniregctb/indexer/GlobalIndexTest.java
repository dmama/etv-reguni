package ch.vd.uniregctb.indexer;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.fs.FSDirectoryProvider;
import ch.vd.uniregctb.indexer.DocGetter;
import ch.vd.uniregctb.indexer.DocHit;
import static junit.framework.Assert.*;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour tester l'indexation de tiers
 *
 * @author <a href="mailto:jean-eric.cuendet@vd.ch">Jean-Eric Cuendet</a>
 */
public class GlobalIndexTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(GlobalIndexTest.class);

	private static final String indexPath = "target/lucene/index";

	private static final String TYPE = "TestDoc";

	private static final String SUBTYPE = "SubTestDoc";

	private static final String TYPE_ALT = "TestDocAlternate";
	private static final String SUBTYPE_ALT = "SubestDocAlternate";

	private static final String[] fieldNames = {
			"TYPE", "SUBTYPE", "NUMERO", "NOM", "RAISON", "DESCR", "DATE"
	};

	private static final String[][] fieldValues = {
			{
					TYPE, SUBTYPE, "1234", "Cuendét Jean-Eric", "JeSC Consulting", "Une société de conseil en informatique", "19740322"
			},
			{
					TYPE, SUBTYPE, "2345", "Lehmann Jean-Pierre", "SoPE", "Solutions pedagogiques", "19520811"
			},
			{
					TYPE, SUBTYPE, "4567", "Mme Cuendet Sara née Barbie", "Sage femmes réunies", "Solutions d'accouchements a la maison",
					"19790223"
			},
			{
					TYPE_ALT, SUBTYPE_ALT, "1234", "Un autre gars", "Une raison", "Corporate engineering2", "20060317"
			},
			{
					TYPE_ALT, SUBTYPE_ALT, "6543", "Bla bli", "Une raison", "Corporate engineering1", "200603"
			},
			{
					TYPE_ALT, SUBTYPE_ALT, "6544", "Corporate society", "Une raison", "Voila engineering", "20060322"
			},
			{
					TYPE_ALT, SUBTYPE_ALT, "6545", "Une raison", "Un corporate building", "Bien au beurre salé", "2006"
			},
			{
					TYPE_ALT, SUBTYPE_ALT, "2345", "Encore une autre raison", "Encore un autre building", "Encore un autre champ",
					"20070127"
			}, {
					TYPE, SUBTYPE, "7373", "Le nom 1", "La raison 1", "Une description: TemaPHilE", "20070127"
			}, {
					TYPE, SUBTYPE, "7374", "Le nom 2", "La raison 2", "Une description: telephone", "20070127"
			},
	};

	// Members
	private GlobalIndexInterface globalIndex;

	private static final SearchCallback NULL_CALLBACK = new SearchCallback() {
		public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
		}
	};

	// Deletes all files and subdirectories under dir.
	// Returns true if all deletions were successful.
	// If a deletion fails, the method stops attempting to delete and returns
	// false.
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(dir, aChildren));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		// Delete the index
		if (new File(indexPath).exists()) {
			boolean success = deleteDir(new File(indexPath));
			if (!success) {
				LOGGER.error("Removing index directory " + indexPath + " failed!");
			}
			assertTrue(success);
		}
		globalIndex = getBean(GlobalIndexInterface.class, "globalIndex");
		globalIndex.overwriteIndex();
		// Index data
		for (String[] vv : fieldValues) {
			Long id = Long.parseLong(vv[2]);
			GenericIndexable indexable = new GenericIndexable(id, vv[0], vv[1], asStringList(fieldNames), asStringList(vv));
			globalIndex.indexEntity(new IndexableData(indexable));
		}

	}

	// Test non utilisé en v2
	public void testSearchBlackListQuery() throws Exception {
		assertHits(2, "NOM:cuendet");
		assertHits(0, "NOM:Mme");
		assertHits(0, "NOM:née");
	}

	@NotTransactional
	@Test
	public void testDocCount() {

		int before = globalIndex.getApproxDocCount();

		assertHits(1, LuceneEngine.F_DOCID + ":" + TYPE + "-1234");

		{
			List<String> fields = new ArrayList<String>();
			fields.add("field1");
			List<String> values = new ArrayList<String>();
			values.add("value1");
			Indexable indexable = new GenericIndexable(123456, "TheType", fields, values);
			globalIndex.indexEntity(new IndexableData(indexable));
		}

		assertHits(1, LuceneEngine.F_DOCID + ":TheType-123456");

		int after1 = globalIndex.getApproxDocCount();
		assertEquals(after1, before + 1);

		{
			List<String> fields = new ArrayList<String>();
			fields.add("field1");
			List<String> values = new ArrayList<String>();
			values.add("value1");
			Indexable indexable = new GenericIndexable(654321, "TheType2", fields, values);
			globalIndex.indexEntity(new IndexableData(indexable));
		}

		assertHits(1, LuceneEngine.F_DOCID + ":TheType2-654321");

		int after2 = globalIndex.getApproxDocCount();
		assertEquals(after2, before + 2);
	}

	/**
	 * @throws Exception
	 */
	@NotTransactional
	@Test
	public void testSearchStringQuery() throws Exception {

		assertHits(1, LuceneEngine.F_DOCID + ":" + TYPE + "-1234");
		assertHits(2, LuceneEngine.F_ENTITYID + ":1234");
		assertHits(5, LuceneEngine.F_DOCTYPE + ":" + TYPE);
		assertHits(1, "DESCR:SoCiété");
		assertHits(2, "NOM:jean");
		assertHits(1, "NOM:GARS");
		assertHits(1, "RAISON:sopE");
		assertHits(1, "NOM:cuenDET AND RAISON:saGe");
		assertHits(1, "DESCR:pedagogiques");
		assertHits(2, "DESCR:Solutions");

		globalIndex.search("DESCR:Solutions AND NOT RAISON:sope", new SearchCallback() {
			public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
				assertEquals(1, hits.size());
				final Document document = docGetter.get(hits.get(0).doc);
				assertEquals("Mme Cuendet Sara née Barbie", document.get("NOM"));
			}
		});

		assertHits(4, "RAISON:corporate OR NOM:corporate OR DESCR:corporate");

		// Query that has no match because Keyword is not found
		assertHits(0, "AJHHFGZDGFGHFGHS:blabla");

		// Empty query => Exception
		try {
			globalIndex.search("", NULL_CALLBACK);
			fail();
		}
		catch (IndexerException e) {
		}

		// Invalid query => Exception
		try {
			globalIndex.search("PRENOM:ali baba", NULL_CALLBACK); // Space is not
			// supported
			fail();
		}
		catch (IndexerException e) {
		}

		// Invalid query => Exception
		try {
			globalIndex.search("PRENOM:*ali*", NULL_CALLBACK); // Etoile au debut
			// est non accepte
			fail();
		}
		catch (IndexerException e) {
		}
	}

	@NotTransactional
	@Test
	public void testSearchProgrammaticQuery() throws IndexerException {

		// Wildcard Query
		{
			Term term = LuceneEngine.getTerm("DESCR", "POR");
			term = new Term(term.field(), "*" + term.text() + "*");
			WildcardQuery baseQuery = new WildcardQuery(term);
			assertHits(2, baseQuery);
		}

		// Wildcard Query
		{
			Term term = LuceneEngine.getTerm("NOM", "DeT");
			term = new Term(term.field(), "*" + term.text());
			WildcardQuery baseQuery = new WildcardQuery(term);
			assertHits(2, baseQuery);
		}

		// Wildcard Query
		{
			Term term = new Term("DESCR", "te*ph*"); // Match: Telephone et
			// Temaphile
			WildcardQuery baseQuery = new WildcardQuery(term);
			assertHits(2, baseQuery);
		}

		{
			BooleanQuery baseQuery = new BooleanQuery();
			baseQuery.add(new TermQuery(LuceneEngine.getTerm("NOM", "CuendeT")), BooleanClause.Occur.MUST);
			baseQuery.add(new TermQuery(LuceneEngine.getTerm("RAISON", "Sage")), BooleanClause.Occur.MUST);
			assertHits(1, baseQuery);
		}

		{
			BooleanQuery baseQuery = new BooleanQuery();
			baseQuery.add(new TermQuery(LuceneEngine.getTerm("NOM", " ")), BooleanClause.Occur.SHOULD);
			baseQuery.add(new TermQuery(LuceneEngine.getTerm("DESCR", "beuRRe")), BooleanClause.Occur.SHOULD);
			assertHits(1, baseQuery);
		}

		{
			BooleanQuery baseQuery = new BooleanQuery();
			baseQuery.add(new TermQuery(LuceneEngine.getTerm("NOM", "Cuendet")), BooleanClause.Occur.MUST);
			baseQuery.add(new TermQuery(LuceneEngine.getTerm("RAISON", "saGE")), BooleanClause.Occur.SHOULD);
			assertHits(2, baseQuery);
		}

		// Complicated with sub-query
		{
			BooleanQuery baseQuery = new BooleanQuery();
			BooleanQuery subQuery1 = new BooleanQuery();
			baseQuery.add(subQuery1, BooleanClause.Occur.SHOULD);
			BooleanQuery subQuery2 = new BooleanQuery();
			baseQuery.add(subQuery2, BooleanClause.Occur.SHOULD);

			subQuery1.add(new TermQuery(LuceneEngine.getTerm("NOM", "Cuendet")), BooleanClause.Occur.MUST);
			subQuery1.add(new TermQuery(LuceneEngine.getTerm("RAISON", "saGE")), BooleanClause.Occur.MUST);
			subQuery2.add(new TermQuery(LuceneEngine.getTerm("NOM", "jean")), BooleanClause.Occur.MUST);
			subQuery2.add(new TermQuery(LuceneEngine.getTerm("RAISON", "sope")), BooleanClause.Occur.MUST);

			assertHits(2, baseQuery);
		}

		// Complicated with sub-query
		{
			BooleanQuery baseQuery = new BooleanQuery();
			BooleanQuery subQuery1 = new BooleanQuery();
			BooleanQuery subQuery2 = new BooleanQuery();

			subQuery1.add(new TermQuery(LuceneEngine.getTerm("NOM", "Cuendet")), BooleanClause.Occur.SHOULD);
			subQuery1.add(new TermQuery(LuceneEngine.getTerm("NOM", "lehmann")), BooleanClause.Occur.SHOULD);
			baseQuery.add(subQuery1, BooleanClause.Occur.MUST);
			subQuery2.add(new TermQuery(LuceneEngine.getTerm("DESCR", "solutions")), BooleanClause.Occur.MUST);
			baseQuery.add(subQuery2, BooleanClause.Occur.MUST_NOT);

			assertHits(1, baseQuery);
		}

	}

	/**
	 * On fait 2 recherches qui renvoie 1 entité et 2 entités On supprime, le nombre de docs dans l'indexer a pas changé On optimize() Le
	 * nombre d'entités dans lîndex doit changer
	 *
	 * @throws IndexerException
	 * @throws ParseException
	 */
	@NotTransactional
	@Test
	public void testRemoveDocumentNeCherchePlus() throws IndexerException, ParseException {

		// Un hit avec TYPE=DocType and ID=1234
		assertHits(1, LuceneEngine.F_DOCID + ":" + TYPE + "-1234");

		// 2 hits avec ID=2345
		assertHits(2, LuceneEngine.F_ENTITYID + ":2345");

		// Remove one HIT (ID=1234 AND type=DocType)
		{
			int before = globalIndex.getApproxDocCount();
			globalIndex.removeEntity(1234L, TYPE);
			globalIndex.optimize(); // Permet que le docCount renvoie juste
			int after = globalIndex.getApproxDocCount();
			assertEquals(before, after + 1);
		}

		// We should have no more document with ID=1234 and TYPE=DocType
		assertHits(0, LuceneEngine.F_DOCID + ":" + TYPE + "-1234");

		// We should have 2 documents with ID=2345
		assertHits(2, LuceneEngine.F_ENTITYID + ":2345");

		// Optimize => docCount est juste maintenant
		{
			int before = globalIndex.getApproxDocCount();
			globalIndex.optimize();
			int after = globalIndex.getApproxDocCount();
			assertEquals(after, before);
		}
	}

	@NotTransactional
	@Test
	public void testRemoveDocument() throws IndexerException, ParseException {

		// First we should have the same number of docs in the 2
		int dc = globalIndex.getApproxDocCount();
		assertEquals(fieldValues.length, dc);

		// Un hit avec TYPE=TYPE and ID=2345
		assertHits(1, LuceneEngine.F_DOCID + ":" + TYPE + "-2345");

		// 2 hits avec ID=1234
		assertHits(2, "NUMERO:1234");

		// 2 hits avec ID=2345
		assertHits(2, "NUMERO:2345");

		// Un hit avec TYPE=DocType and ID=1234
		assertHits(1, LuceneEngine.F_DOCID + ":" + TYPE + "-1234");

		// Remove one HIT (ID=1234 AND type=DocType)
		{
			int before = globalIndex.getApproxDocCount();
			globalIndex.removeEntity(1234L, TYPE);
			globalIndex.optimize();
			int after = globalIndex.getApproxDocCount();
			assertEquals(after, before - 1);
			assertEquals(fieldValues.length - 1, globalIndex.getApproxDocCount());
		}

		// We should have no more document with ID=1234 and TYPE=DocType
		assertHits(0, LuceneEngine.F_DOCID + ":" + TYPE + "-1234");

		// We should have one document with ID=2345
		assertHits(1, LuceneEngine.F_ENTITYID + ":1234");

		// Remove 2 hits (ID=2345)
		{
			int before = globalIndex.getApproxDocCount();
			globalIndex.removeEntity(2345L, TYPE);
			globalIndex.optimize(); // Delete pour de vrai
			int after = globalIndex.getApproxDocCount();
			assertEquals(after, before - 1);
			assertEquals(fieldValues.length - 2, globalIndex.getApproxDocCount());
		}

		// We should have no more document with ID=2345
		assertHits(1, "NUMERO:2345");
	}

	@Test
	@NotTransactional
	public void testReplaceDocument() throws IndexerException, ParseException {

		// Un hit avec ID=1234
		assertHits(1, "NUMERO:1234 AND TYPE:" + TYPE);

		// Un hit avec Jean-Eric
		assertHits(1, "NOM:Jean-Eric");

		// Pas de hit d'abord avec andré
		assertHits(0, "NOM:andré");

		// Incremental indexer
		{
			String[] oldValues = fieldValues[0];
			Long id = Long.parseLong(oldValues[2]);
			String[] newValues = {
					oldValues[0], oldValues[1], id.toString(), "Cuendet Marc-André", "JeSC Corporation",
					"Une société de conseil en informatique", "20020123"
			};
			GenericIndexable indexable = new GenericIndexable(id.longValue(), newValues[0], newValues[1], asStringList(fieldNames),
					asStringList(newValues));
			// This should replace the "Cuendet jean-Eric" doc with ID=1234
			globalIndex.removeThenIndexEntity(new IndexableData(indexable));
		}

		// Un hit avec ID=1234
		assertHits(1, "NUMERO:1234 AND TYPE:" + TYPE);

		// Zero hit avec Jean-Eric
		assertHits(0, "NOM:Jean-Eric");

		// Ensuite un hit avec les nouvelles infos
		assertHits(1, "NOM:andré");
	}

	@NotTransactional
	@Test
	public void testDateNaissance() throws Exception {

		// Un hit avec DATE=22 mars 1974
		assertHits(1, "DATE:19740322");
		// X hits avec DATE=2006*
		assertHits(4, "DATE:2006*");
		// 1 hit avec DATE=2006
		assertHits(1, "DATE:2006");
		// 1 hit avec DATE=200603
		assertHits(1, "DATE:200603");
		// 3 hits avec DATE=200603*
		assertHits(3, "DATE:200603*");
	}

	private void assertHits(final int count, Query query) {
		globalIndex.search(query, new SearchCallback() {
			public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
				assertEquals(count, hits.size());
			}
		});
	}

	private void assertHits(final int count, String query) {
		globalIndex.search(query, new SearchCallback() {
			public void handle(List<DocHit> hits, DocGetter docGetter) throws Exception {
				assertEquals(count, hits.size());
			}
		});
	}

	/**
	 * Teste que le global index est thread-safe
	 */
	@Test
	public void testMultithreadAccess() throws Exception {

		Runnable command = new Runnable() {
			public void run() {
				for (int i = 1; i < 10; ++i) {
					for (String[] vv : fieldValues) {
						Long id = Long.parseLong(vv[2]);
						GenericIndexable indexable = new GenericIndexable(id, vv[0], vv[1], asStringList(fieldNames), asStringList(vv));
						globalIndex.indexEntity(new IndexableData(indexable));
					}
				}
			}
		};

		final List<Throwable> throwables = new ArrayList<Throwable>();

		UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				throwables.add(e);
			}
		};

		// on crée 4 threads qui vont réindexer toutes les données en parallèle
		Thread thread1 = new Thread(command);
		Thread thread2 = new Thread(command);
		Thread thread3 = new Thread(command);
		Thread thread4 = new Thread(command);
		thread1.setUncaughtExceptionHandler(handler);
		thread2.setUncaughtExceptionHandler(handler);
		thread3.setUncaughtExceptionHandler(handler);
		thread4.setUncaughtExceptionHandler(handler);

		thread1.start();
		thread2.start();
		thread3.start();
		thread4.start();

		thread1.join();
		thread2.join();
		thread3.join();
		thread4.join();

		for (Throwable t : throwables) {
			LOGGER.error(t, t);
		}
		assertEmpty(throwables);
	}

	/**
	 * Teste que le global index est process-safe
	 * <p>
	 * Note: on ne peut pas créer facilement des sous-processus avec Java (fork() ne fonctionne à l'intérieur d'une JVM). Il est possible de
	 * démarrer des processus, mais il faut spécifier la ligne de commande complète, et ce n'est pas vraiment pas pratique dans le cadre de
	 * tests unitaires.
	 * <p>
	 * Alors, on triche un peu et on contourne la synchronisation mise en place par le GlobalIndex en créant à la main des IndexWriter
	 * depuis plusieurs threads. Dans ces conditions, plusieurs IndexWriters vont essayer d'accéder au répertoire d'index sur le disque, ce
	 * qui correspond à la configuration de plusieurs processes (Unireg + Unireg interface) se partageant le même index.
	 */
	@Test
	public void testMultiprocessAccess() throws Exception {

		// crée un index local de manière à pouvoir utiliser les méthodes protégées
		final DirectoryProvider provider = new FSDirectoryProvider(indexPath);
		final GlobalIndex localIndex = new GlobalIndex(provider);
		localIndex.afterPropertiesSet();

		Runnable command = new Runnable() {
			public void run() {

				for (int i = 1; i < 10; ++i) {
					for (String[] vv : fieldValues) {
						Long id = Long.parseLong(vv[2]);
						GenericIndexable indexable = new GenericIndexable(id, vv[0], vv[1], asStringList(fieldNames), asStringList(vv));

						/*
						 * on contourne la synchronisation de globalIndex.indexEntity(indexable), de manière simuler plusieurs processus
						 * utilisant des writers
						 */
						LuceneWriter writer = null;
						try {
							writer = new LuceneWriter(localIndex.directory.directory, false);
							writer.index(indexable.getID(), indexable.getType(), indexable.getSubType(), indexable.getKeyValues());
						}
						finally {
							if (writer != null) {
								writer.close();
							}
						}
					}
				}
			}
		};

		final List<Throwable> throwables = new ArrayList<Throwable>();

		UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				throwables.add(e);
			}
		};

		// on crée 4 threads qui vont réindexer toutes les données en parallèle
		Thread thread1 = new Thread(command);
		Thread thread2 = new Thread(command);
		Thread thread3 = new Thread(command);
		Thread thread4 = new Thread(command);
		thread1.setUncaughtExceptionHandler(handler);
		thread2.setUncaughtExceptionHandler(handler);
		thread3.setUncaughtExceptionHandler(handler);
		thread4.setUncaughtExceptionHandler(handler);

		thread1.start();
		thread2.start();
		thread3.start();
		thread4.start();

		thread1.join();
		thread2.join();
		thread3.join();
		thread4.join();

		for (Throwable t : throwables) {
			LOGGER.error(t, t);
		}
		assertEmpty(throwables);
	}
}
