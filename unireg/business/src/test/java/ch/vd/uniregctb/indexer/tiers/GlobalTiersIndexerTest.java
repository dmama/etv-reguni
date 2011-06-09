package ch.vd.uniregctb.indexer.tiers;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.Sexe;

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
	@NotTransactional
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
	@NotTransactional
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
	@NotTransactional
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
	@NotTransactional
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
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jean", "Test", date(1960, 3, 3), Sexe.MASCULIN);
				pp.scheduleReindexationOn(reindexOn);
				return pp.getNumero();
			}
		});
	}

	private void resetDirtyFlag(final Long id) throws Exception {
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				hibernateTemplate.execute(new HibernateCallback<Object>() {
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
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
				assertNotNull(pp);
				assertEquals(isDirty, pp.isDirty());
				assertEquals(reindexOn, pp.getReindexOn());
				return null;
			}
		});
	}
}
