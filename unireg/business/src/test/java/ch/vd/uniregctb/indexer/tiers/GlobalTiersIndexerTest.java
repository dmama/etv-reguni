package ch.vd.uniregctb.indexer.tiers;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.indexer.GlobalIndex;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.async.MassTiersIndexer;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.worker.BatchWorker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class GlobalTiersIndexerTest extends BusinessTest {

	private GlobalTiersIndexerImpl indexer;
	private GlobalTiersSearcher searcher;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		indexer = getBean(GlobalTiersIndexerImpl.class, "globalTiersIndexer");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

		indexer.overwriteIndex();
		setWantIndexationTiers(false); // -> va mettre le flag dirty sur tous les tiers modifiés
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty va bien réindexer un tiers non-dirty avec une date de réindexation schedulée dans le passé. Après l'exécution, le tiers doit avoir été indexé, il
	 * ne doit plus être dirty et la date de schedule doit être nulle.
	 */
	@Test
	public void testIndexDirtyTiersNonDirtyAvecDateReindexOnDansLePasse() throws Exception {

		final RegDate ilya10jours = RegDate.get().addDays(-10);

		// Crée un tiers non-dirty qui devait être réindexé il y a 10 jours
		final Long id = createTiers(ilya10jours);
		resetDirtyFlag(id);

		// On vérifie qu'on est bien dans l'état désiré
		assertTiers(false, ilya10jours, id);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(id);
		assertEmpty(searcher.search(criteria));

		// On effectue un réindexation des dirties (ce qui inclut les tiers schedulés pour être réindexés)
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY);

		// On vérifie que le tiers n'est plus schedulé pour être réindexé
		assertTiers(false, null, id);

		// On vérifie que le tiers est maintenant bien indexé
		final List<TiersIndexedData> list = searcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(id, list.get(0).getNumero());
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty va bien réindexer un tiers non-dirty avec une date de réindexation schedulée pour le jour même. Après l'exécution, le tiers doit avoir été indexé,
	 * il ne doit plus être dirty et la date de schedule doit être nulle.
	 */
	@Test
	public void testIndexDirtyTiersNonDirtyAvecDateReindexOnAujourdhui() throws Exception {

		final RegDate aujourdhui = RegDate.get();

		// Crée un tiers non-dirty qui doit être réindexé aujourd'hui
		final Long id = createTiers(aujourdhui);
		resetDirtyFlag(id);

		// On vérifie qu'on est bien dans l'état désiré
		assertTiers(false, aujourdhui, id);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(id);
		assertEmpty(searcher.search(criteria));

		// On effectue un réindexation des dirties (ce qui inclut les tiers schedulés pour être réindexés)
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY);

		// On vérifie que le tiers n'est plus schedulé pour être réindexé
		assertTiers(false, null, id);

		// On vérifie que le tiers est maintenant bien indexé
		final List<TiersIndexedData> list = searcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(id, list.get(0).getNumero());
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty va bien réindexer un tiers dirty malgré une date de réindexation schedulée dans le futur. Après l'exécution, le tiers doit avoir été réindexé, il
	 * ne doit plus être dirty <b>et</b> la date de schedule doit être inchangée.
	 */
	@Test
	public void testIndexDirtyTiersDirtyAvecDateReindexOnDansLeFutur() throws Exception {

		final RegDate dans10jours = RegDate.get().addDays(10);

		// Crée un tiers dirty *et* qui doit être réindexé dans 10 jours
		final Long id = createTiers(dans10jours);

		// On vérifie qu'on est bien dans l'état désiré
		assertTiers(true, dans10jours, id);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(id);
		assertEmpty(searcher.search(criteria));

		// On effectue un réindexation des dirties
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY);

		// On vérifie que le tiers n'est plus dirty, mais qu'il est toujours schedulé pour être réindexé dans le futur
		assertTiers(false, dans10jours, id);

		// On vérifie que le tiers est maintenant bien indexé
		final List<TiersIndexedData> list = searcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(id, list.get(0).getNumero());
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty ne réindexe pas un tiers non-dirty avec une date de réindexation schedulée dans le futur. Après l'exécution, le tiers ne doit pas avoir été
	 * réindexé, il ne doit pas être dirty <b>et</b> la date de schedule doit être inchangée.
	 */
	@Test
	public void testIndexDirtyTiersNonDirtyAvecDateReindexOnDansLeFutur() throws Exception {

		final RegDate dans10jours = RegDate.get().addDays(10);

		// Crée un tiers non-dirty *et* qui doit être réindexé dans 10 jours
		final Long id = createTiers(dans10jours);
		resetDirtyFlag(id);

		// On vérifie qu'on est bien dans l'état désiré
		assertTiers(false, dans10jours, id);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(id);
		assertEmpty(searcher.search(criteria));

		// On effectue un réindexation des dirties
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY);

		// On vérifie que le tiers n'est plus dirty, mais qu'il est toujours schedulé pour être réindexé dans le futur
		assertTiers(false, dans10jours, id);

		// On vérifie que le tiers n'a pas été réindexé
		final List<TiersIndexedData> list = searcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(id, list.get(0).getNumero());
	}

	private Long createTiers(final RegDate reindexOn) throws Exception {
		return doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jean", "Test", date(1960, 3, 3), Sexe.MASCULIN);
				pp.scheduleReindexationOn(reindexOn);
				return pp.getNumero();
			}
		});
	}

	private void resetDirtyFlag(final Long id) throws Exception {
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				hibernateTemplate.execute(new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						final SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(false) + " where NUMERO = " + id);
						query.executeUpdate();
						return null;
					}
				});
				return null;
			}
		});
	}

	private void assertTiers(final boolean isDirty, final RegDate reindexOn, final Long id) throws Exception {
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
				assertNotNull(pp);
				assertEquals(isDirty, pp.isDirty());
				assertEquals(reindexOn, pp.getReindexOn());
				return null;
			}
		});
	}

	@Test(timeout = 60000)
	public void testArretJobSiTousLesThreadsTombent() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});

		// "Bidouille" pour lancer une indexation pour laquelle les threads tombent avant d'avoir fini
		final SessionFactory sessionFactory = getBean(SessionFactory.class, "sessionFactory");
		final GlobalTiersIndexerImpl indexer = new GlobalTiersIndexerImpl() {
			@Override
			protected MassTiersIndexer createMassTiersIndexer(int nbThreads, @NotNull Mode mode, int queueSizeByThread) {
				return new MassTiersIndexer(nbThreads, queueSizeByThread, new BatchWorker<Long>() {
					@Override
					public void process(List<Long> data) throws Exception {
						// boom au bout d'une seconde déjà...
						try {
							Thread.sleep(1000);
							throw new RuntimeException("Boom ! (exception de test)");
						}
						catch (InterruptedException e) {
							Assert.fail("Thread interrompu!");
						}
					}

					@Override
					public int maxBatchSize() {
						return 100;
					}

					@Override
					public String getName() {
						return "TestMass";
					}
				});
			}

			@Override
			protected Duration getOfferTimeout() {
				return Duration.ofSeconds(2);       // pour accélérer un peu le mouvement dans les tests
			}
		};

		indexer.setAdresseService(getBean(AdresseService.class, "adresseService"));
		indexer.setGlobalIndex(getBean(GlobalIndex.class, "globalTiersIndex"));
		indexer.setStatsService(getBean(StatsService.class, "statsService"));
		indexer.setServiceCivilService(serviceCivil);
		indexer.setServiceInfra(serviceInfra);
		indexer.setSessionFactory(sessionFactory);
		indexer.setTiersDAO(tiersDAO);
		indexer.setTiersSearcher(globalTiersSearcher);
		indexer.setTiersService(tiersService);
		indexer.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		indexer.setTransactionManager(transactionManager);
		indexer.afterPropertiesSet();

		// initialisation de la DB avec plein de tiers à indexer
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				// j'en mets beaucoup pour me placer dans la position où la queue est pleine
				// alors que les threads d'indexation sont morts
				for (int i = 0 ; i < 2000 ; ++ i) {
					addNonHabitant(null, getNom(i), null, getSexe(i));
				}
				return null;
			}

			private String getNom(int i) {
				final char[] value = Integer.toString(i).toCharArray();
				for (int pos = 0 ; pos < value.length ; ++ pos) {
					value[pos] += 'A' - '0';
				}
				return new String(value);
			}

			private Sexe getSexe(int i) {
				return i % 2 == 0 ? Sexe.MASCULIN : Sexe.FEMININ;
			}
		});

		try {
			indexer.indexAllDatabase(null, 4, GlobalTiersIndexer.Mode.FULL);
			Assert.fail("Comment ça, tout s'est bien passé ???");
		}
		catch (IndexerException e) {
			Assert.assertTrue(e.getMessage().contains("Les threads d'indexation se sont tous arrêtés"));
		}
		finally {
			indexer.destroy();
		}
	}

	/**
	 * [UNIREG-1979] Vérifie que l'indexation des dirty va bien réindexer un tiers dirty malgré une date de réindexation schedulée dans le futur. Après l'exécution, le tiers doit avoir été réindexé, il
	 * ne doit plus être dirty <b>et</b> la date de schedule doit être inchangée.
	 */
	@Test
	public void testDeleteEntitiesIndexedBefore() throws Exception {

		setWantIndexationTiers(true);

		// on indexe deux personnes
		doInNewTransaction(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Jean", "Test1", date(1960, 3, 3), Sexe.MASCULIN);
			return pp1.getId();
		});
		doInNewTransaction(status -> {
			final PersonnePhysique pp2 = addNonHabitant("Jean", "Test2", date(1960, 3, 3), Sexe.MASCULIN);
			return pp2.getId();
		});

		indexer.sync();

		// on indexe une personne supplémentaire
		final Date dateAvantPP3 = new Date();
		doInNewTransaction(status -> {
			final PersonnePhysique pp3 = addNonHabitant("Jean", "Test3", date(1960, 3, 3), Sexe.MASCULIN);
			return pp3.getId();
		});

		indexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Jean");

		// on vérifie que les trois personnes sont dans l'indexe
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test1", results.get(0).getNom1());
			assertEquals("Jean Test2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}

		// on efface les tiers indexés avant le tiers PP3
		indexer.deleteEntitiesIndexedBefore(dateAvantPP3);
		indexer.sync();

		// on vérifie que seule la personne PP3 reste
		{
			assertEquals(1, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(1, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test3", results.get(0).getNom1());
		}
	}

	/**
	 * Ce test vérifie que le mode FULL fonctionne bien.
	 */
	@Test
	public void testIndexAllDatabaseFull() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});

		truncateDatabase();
		setWantIndexationTiers(true);

		// on indexe trois personnes
		final Long test1Id = doInNewTransaction(status -> {
			final PersonnePhysique test1 = addNonHabitant("Jean", "Test1", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test2", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test3", date(1960, 3, 3), Sexe.MASCULIN);
			return test1.getId();
		});
		indexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Jean");

		// on vérifie que les trois personnes sont dans l'indexe
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test1", results.get(0).getNom1());
			assertEquals("Jean Test2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}

		setWantIndexationTiers(false);

		// on supprime Test1 et on ajoute Test4
		doInNewTransaction(status -> {
			tiersDAO.remove(test1Id);
			addNonHabitant("Jean", "Test4", date(1960, 3, 3), Sexe.MASCULIN);
			return null;
		});

		// l'indexe doit être inchangé
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test1", results.get(0).getNom1());
			assertEquals("Jean Test2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}

		// on réindexe en mode FULL_INCREMENTAL
		final int nbIndexed = indexer.indexAllDatabase(null, 4, GlobalTiersIndexer.Mode.FULL);
		assertEquals(3, nbIndexed);

		// on vérifie que Test2, Test3 et Test4 sont maintenant indexés et que Test1 ne l'est plus
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test2", results.get(0).getNom1());
			assertEquals("Jean Test3", results.get(1).getNom1());
			assertEquals("Jean Test4", results.get(2).getNom1());
		}
	}

	/**
	 * [SIFISC-27025] Ce test vérifie que le mode FULL_INCREMENTAL fonctionne bien.
	 */
	@Test
	public void testIndexAllDatabaseFullIncremental() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});

		truncateDatabase();
		setWantIndexationTiers(true);

		// on indexe trois personnes
		final Long test1Id = doInNewTransaction(status -> {
			final PersonnePhysique test1 = addNonHabitant("Jean", "Test1", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test2", date(1960, 3, 3), Sexe.MASCULIN);
			addNonHabitant("Jean", "Test3", date(1960, 3, 3), Sexe.MASCULIN);
			return test1.getId();
		});
		indexer.sync();

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison("Jean");

		// on vérifie que les trois personnes sont dans l'indexe
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test1", results.get(0).getNom1());
			assertEquals("Jean Test2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}

		setWantIndexationTiers(false);

		// on supprime Test1 et on ajoute Test4
		doInNewTransaction(status -> {
			tiersDAO.remove(test1Id);
			addNonHabitant("Jean", "Test4", date(1960, 3, 3), Sexe.MASCULIN);
			return null;
		});

		// l'indexe doit être inchangé
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test1", results.get(0).getNom1());
			assertEquals("Jean Test2", results.get(1).getNom1());
			assertEquals("Jean Test3", results.get(2).getNom1());
		}

		// on réindexe en mode FULL_INCREMENTAL
		final int nbIndexed = indexer.indexAllDatabase(null, 4, GlobalTiersIndexer.Mode.FULL_INCREMENTAL);
		assertEquals(3, nbIndexed);

		// on vérifie que Test2, Test3 et Test4 sont maintenant indexés et que Test1 ne l'est plus
		{
			assertEquals(3, searcher.getExactDocCount());
			final List<TiersIndexedData> results = searcher.search(criteria);
			assertEquals(3, results.size());
			results.sort(Comparator.comparing(TiersIndexedData::getNom1));
			assertEquals("Jean Test2", results.get(0).getNom1());
			assertEquals("Jean Test3", results.get(1).getNom1());
			assertEquals("Jean Test4", results.get(2).getNom1());
		}
	}
}
