package ch.vd.unireg.indexer.tiers;

import java.util.Comparator;
import java.util.List;

import org.hibernate.query.NativeQuery;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.Sexe;

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
		indexer.indexAllDatabase(GlobalTiersIndexer.Mode.DIRTY_ONLY, 1, null);

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
		indexer.indexAllDatabase(GlobalTiersIndexer.Mode.DIRTY_ONLY, 1, null);

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
		indexer.indexAllDatabase(GlobalTiersIndexer.Mode.DIRTY_ONLY, 1, null);

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
		indexer.indexAllDatabase(GlobalTiersIndexer.Mode.DIRTY_ONLY, 1, null);

		// On vérifie que le tiers n'est plus dirty, mais qu'il est toujours schedulé pour être réindexé dans le futur
		assertTiers(false, dans10jours, id);

		// On vérifie que le tiers n'a pas été réindexé
		final List<TiersIndexedData> list = searcher.search(criteria);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(id, list.get(0).getNumero());
	}

	private Long createTiers(final RegDate reindexOn) throws Exception {
		return doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jean", "Test", date(1960, 3, 3), Sexe.MASCULIN);
			pp.scheduleReindexationOn(reindexOn);
			return pp.getNumero();
		});
	}

	private void resetDirtyFlag(final Long id) throws Exception {
		doInNewTransactionAndSession(status -> {
			hibernateTemplate.execute(session -> {
				final NativeQuery query = session.createNativeQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(false) + " where NUMERO = " + id);
				query.executeUpdate();
				return null;
			});
			return null;
		});
	}

	private void assertTiers(final boolean isDirty, final RegDate reindexOn, final Long id) throws Exception {
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(pp);
			assertEquals(isDirty, pp.isDirty());
			assertEquals(reindexOn, pp.getReindexOn());
			return null;
		});
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
		final int nbIndexed = indexer.indexAllDatabase(GlobalTiersIndexer.Mode.FULL, 4, null);
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
