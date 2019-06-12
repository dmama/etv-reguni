package ch.vd.unireg.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.ProgressMonitor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ParallelBatchTransactionTemplateWithResultsTest extends BusinessTest {

	private static class TestJobResults implements BatchResults<Long, TestJobResults> {
		@Override
		public void addErrorException(Long element, Exception e) {
		}

		@Override
		public void addAll(TestJobResults right) {
		}
	}

	@Test
	public void testEmptyList() {
		List<Long> list = Collections.emptyList();
		final TestJobResults rapportFinal = new TestJobResults();
		ParallelBatchTransactionTemplateWithResults<Long, TestJobResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 100, 2, Behavior.SANS_REPRISE, transactionManager, null, AuthenticationInterface.INSTANCE);
		final boolean completed = template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public void beforeTransaction() {
				fail();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				fail();
				return true;
			}

			@Override
			public void afterTransactionCommit() {
				fail();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail();
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		assertTrue(completed);
	}

	@Test
	public void testEverythingProcessed() {

		final List<Long> list = generateList(1000);
		final Set<Long> processed = new HashSet<>(1000);

		// processe 1000 longs par lots de 10 en 5 threads différents
		final TestJobResults rapportFinal = new TestJobResults();
		final ParallelBatchTransactionTemplateWithResults<Long, TestJobResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 10, 5, Behavior.SANS_REPRISE, transactionManager, null, AuthenticationInterface.INSTANCE);
		final boolean completed = template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				synchronized (processed) {
					processed.addAll(batch);
				}
				return true;
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		// on s'assure que tous les ids ont été processés
		assertTrue(completed);
		assertTrue(processed.containsAll(list));
	}

	/**
	 * Vérifie que le template s'arrête si tous les threads sont morts avant d'avoir terminé le processing
	 */
	@Test(timeout = 5000)
	public void testDetectDeadThreads() {

		final List<Long> list = generateList(1000);

		final TestJobResults rapportFinal = new TestJobResults();
		final ParallelBatchTransactionTemplateWithResults<Long, TestJobResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 100, 2, Behavior.SANS_REPRISE, transactionManager, null, AuthenticationInterface.INSTANCE);
		try {
			template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

				@Override
				public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
					return true;
				}

				@Override
				public void afterTransactionCommit() {
					throw new RuntimeException("forced halt");
				}

				@Override
				public TestJobResults createSubRapport() {
					return new TestJobResults();
				}
			}, null);
		}
		catch (Exception e) {
			assertEquals("java.lang.RuntimeException: forced halt", e.getMessage());
		}
	}

	/**
	 * Vérifie que le template s'arrête sans erreur si le traitement est interrompu avec le status manager
	 */
	@Test
	public void testStatusManagerInterrupted() {

		final List<Long> list = generateList(1000);

		final MutableBoolean interrupted = new MutableBoolean(false);
		final StatusManager status = new StatusManager() {
			@Override
			public boolean isInterrupted() {
				return interrupted.getValue();
			}

			@Override
			public void setMessage(String msg) {
			}

			@Override
			public void setMessage(String msg, int percentProgression) {
			}
		};

		final Set<Long> processed = new HashSet<>();
		final TestJobResults rapportFinal = new TestJobResults();
		final ParallelBatchTransactionTemplateWithResults<Long, TestJobResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 100, 2, Behavior.SANS_REPRISE, transactionManager, status, AuthenticationInterface.INSTANCE);
		final boolean completed = template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				processed.addAll(batch);
				interrupted.setValue(true); // interrompt le traitement
				return true;
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		assertFalse(completed);
		assertTrue(!processed.isEmpty()); // le traitement a bien commencé
		assertTrue(processed.size() < 1000); // le traitement s'est bien interrompu
	}

	/**
	 * Vérifie que le template s'arrête sans erreur si le traitement est interrompu avec un retour de 'execute' à false.
	 */
	@Test
	public void testExecutionStopped() {

		final List<Long> list = generateList(1000);

		final TestJobResults rapportFinal = new TestJobResults();
		final Set<Long> processed = new HashSet<>();
		ParallelBatchTransactionTemplateWithResults<Long, TestJobResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 100, 2, Behavior.SANS_REPRISE, transactionManager, null, AuthenticationInterface.INSTANCE);
		final boolean completed = template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				processed.addAll(batch);
				Thread.sleep(100);      // on force le thread a rendre la main afin que le thread de contrôle voie rapidement que les threads renvoient false
				return false; // interrompt le traitement
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		assertFalse(completed);
		assertTrue(!processed.isEmpty()); // le traitement a bien commencé
		assertTrue(processed.size() < 1000); // le traitement s'est bien interrompu
	}

	private static List<Long> generateList(int count) {
		List<Long> list = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			list.add((long) i);
		}
		return list;
	}

	/**
	 * Vérifie que la génération du rapport final fonctionne correctement dans le cas simple (sans rollback)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationRapportSansRollback() {

		final int count = 5000;
		final int nbThreads = 10;

		final List<Long> list = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			list.add((long) i);
		}

		final Rapport rapportFinal = new Rapport();
		final ParallelBatchTransactionTemplateWithResults<Long, Rapport> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 10, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, Rapport>() {

			@Override
			public Rapport createSubRapport() {
				return new Rapport();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, Rapport rapport) throws Exception {
				for (Long element : batch) {
					rapport.addTraite(element);
				}
				return true;
			}
		}, null);

		assertEquals(count, rapportFinal.traites.size());
		assertEmpty(rapportFinal.erreurs);
	}

	/**
	 * Vérifie que la génération du rapport final fonctionne correctement avec des rollbacks
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationRapportAvecRollback() {

		final int count = 5000;
		final int nbThreads = 10;

		final List<Long> list = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			list.add((long) i);
		}

		final Rapport rapportFinal = new Rapport();
		final ParallelBatchTransactionTemplateWithResults<Long, Rapport> template =
				new ParallelBatchTransactionTemplateWithResults<>(list, 10, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, Rapport>() {

			@Override
			public Rapport createSubRapport() {
				return new Rapport();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, Rapport rapport) throws Exception {
				for (Long element : batch) {
					rapport.addTraite(element);
					if (element == 13 || element == 23) { // on fait sauter les éléments 13 et 23
						throw new RuntimeException();
					}
					// note : le rapport final ne doit pas contenir les éléments en erreur, même s'ils ont été ajoutés au rapport intermédiaire.
				}

				return true;
			}
		}, null);

		assertEquals(count - 2, rapportFinal.traites.size());
		assertEquals(2, rapportFinal.erreurs.size());
		assertTrue(rapportFinal.erreurs.contains((long) 13));
		assertTrue(rapportFinal.erreurs.contains((long) 23));
	}

	private static class Rapport implements BatchResults<Long, Rapport> {

		public Set<Long> traites = new HashSet<>();
		public Set<Long> erreurs = new HashSet<>();

		public void addTraite(Long element) {
			traites.add(element);
		}

		@Override
		public void addErrorException(Long element, Exception e) {
			erreurs.add(element);
		}

		@Override
		public void addAll(Rapport right) {
			this.traites.addAll(right.traites);
			this.erreurs.addAll(right.erreurs);
		}
	}

	@Test
	public void testProgressMonitoring() throws Exception {

		final BatchWithResultsCallback<Long, TestJobResults> callback = new BatchWithResultsCallback<Long, TestJobResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				Thread.sleep(10);
				return true;
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		};

		// on envoie la sauce... 50 étapes
		final int TOTAL = 100;
		final int BATCH_SIZE = 2;
		final int NB_THREADS = 5;

		final Set<Integer> collector = Collections.synchronizedSet(new TreeSet<>(Arrays.asList(0, 100)));
		final List<Long> data = new ArrayList<>(TOTAL);
		for (long i = 0 ; i < TOTAL ; ++ i) {
			data.add(i);
		}

		final ProgressMonitor pm = percent -> collector.add(percent);

		final TestJobResults rapportFinal = new TestJobResults();
		final ParallelBatchTransactionTemplateWithResults<Long, TestJobResults>
				template = new ParallelBatchTransactionTemplateWithResults<>(data, BATCH_SIZE, NB_THREADS, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, callback, pm);

		final int expectedSetSize = TOTAL / BATCH_SIZE + 1;
		final Integer[] expectedSetContent = new Integer[expectedSetSize];
		for (int i = 0 ; i < expectedSetSize ; ++ i) {
			expectedSetContent[i] = 2*i;
		}
		assertArrayEquals(expectedSetContent, collector.toArray(new Integer[collector.size()]));
	}
}
