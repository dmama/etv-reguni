package ch.vd.uniregctb.indexer.tiers;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.GlobalIndex;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.async.MassTiersIndexer;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.worker.BatchWorker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class GlobalTiersIndexerTest extends BusinessTest {

	private GlobalTiersIndexer indexer;
	private GlobalTiersSearcher searcher;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		indexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

		indexer.overwriteIndex();
		setWantIndexation(false); // -> va mettre le flag dirty sur tous les tiers modifiés
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
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY, false);

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
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY, false);

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
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY, false);

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
		indexer.indexAllDatabase(null, 1, GlobalTiersIndexer.Mode.DIRTY_ONLY, false);

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
			protected MassTiersIndexer createMassTiersIndexer(int nbThreads, Mode mode, int queueSizeByThread, boolean prefetchIndividus) {
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
			protected int getOfferTimeoutInSeconds() {
				return 2;       // pour accélérer un peu le mouvement dans les tests
			}
		};

		indexer.setAdresseService(getBean(AdresseService.class, "adresseService"));
		indexer.setGlobalIndex(getBean(GlobalIndex.class, "globalIndex"));
		indexer.setStatsService(getBean(StatsService.class, "statsService"));
		indexer.setServiceCivilService(serviceCivil);
		indexer.setServiceInfra(serviceInfra);
		indexer.setSessionFactory(sessionFactory);
		indexer.setTiersDAO(tiersDAO);
		indexer.setTiersSearcher(globalTiersSearcher);
		indexer.setTiersService(tiersService);
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
			indexer.indexAllDatabase(null, 4, GlobalTiersIndexer.Mode.FULL, true);
			Assert.fail("Comment ça, tout s'est bien passé ???");
		}
		catch (IndexerException e) {
			Assert.assertTrue(e.getMessage().contains("Les threads d'indexation se sont tous arrêtés"));
		}
		finally {
			indexer.destroy();
		}
	}
}
