package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ParallelBatchTransactionTemplateTest extends BusinessTest {


	@Test
	public void testEmptyList() {
		List<Long> list = Collections.emptyList();
		ParallelBatchTransactionTemplate<Long, JobResults> template =
				new ParallelBatchTransactionTemplate<>(list, 100, 2, BatchTransactionTemplate.Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		final boolean completed = template.execute(new BatchTransactionTemplate.BatchCallback<Long, JobResults>() {

			@Override
			public void beforeTransaction() {
				fail();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
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
		});

		assertTrue(completed);
	}

	@Test
	public void testEverythingProcessed() {

		final List<Long> list = generateList(1000);
		final Set<Long> processed = new HashSet<>(1000);

		// processe 1000 longs par lots de 10 en 5 threads différents
		ParallelBatchTransactionTemplate<Long, JobResults> template =
				new ParallelBatchTransactionTemplate<>(list, 10, 5, BatchTransactionTemplate.Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		final boolean completed = template.execute(new BatchTransactionTemplate.BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				synchronized (processed) {
					processed.addAll(batch);
				}
				return true;
			}
		});

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

		final ParallelBatchTransactionTemplate<Long, JobResults> template =
				new ParallelBatchTransactionTemplate<>(list, 100, 2, BatchTransactionTemplate.Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchTransactionTemplate.BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				return true;
			}

			@Override
			public void afterTransactionCommit() {
				throw new RuntimeException("forced halt");
			}
		});
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
			public boolean interrupted() {
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
		ParallelBatchTransactionTemplate<Long, JobResults> template =
				new ParallelBatchTransactionTemplate<>(list, 100, 2, BatchTransactionTemplate.Behavior.SANS_REPRISE, transactionManager, status, hibernateTemplate);
		final boolean completed = template.execute(new BatchTransactionTemplate.BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				processed.addAll(batch);
				interrupted.setValue(true); // interrompt le traitement
				return true;
			}
		});

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

		final Set<Long> processed = new HashSet<>();
		ParallelBatchTransactionTemplate<Long, JobResults> template =
				new ParallelBatchTransactionTemplate<>(list, 100, 2, BatchTransactionTemplate.Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		final boolean completed = template.execute(new BatchTransactionTemplate.BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				processed.addAll(batch);
				return false; // interrompt le traitement
			}
		});

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

		ParallelBatchTransactionTemplate<Long, Rapport> template =
				new ParallelBatchTransactionTemplate<>(list, 10, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, Rapport>() {

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
		});

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

		ParallelBatchTransactionTemplate<Long, Rapport> template =
				new ParallelBatchTransactionTemplate<>(list, 10, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, Rapport>() {

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
		});

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

		final BatchTransactionTemplate.BatchCallback<Integer, BatchResults> callback = new BatchTransactionTemplate.BatchCallback<Integer, BatchResults>() {
			@Override
			public boolean doInTransaction(List<Integer> batch, BatchResults rapport) throws Exception {
				Thread.sleep(10);
				return true;
			}
		};

		final Set<Integer> collector = new TreeSet<>(Arrays.asList(0, 100));
		final Mutable<Boolean> started = new MutableBoolean(false);
		final TimerTask samplingTask = new TimerTask() {
			@Override
			public void run() {
				final int percent = callback.percent;
				collector.add(percent);
				started.setValue(true);
			}
		};

		// on envoie la sauce... 50 étapes
		final int TOTAL = 100;
		final int BATCH_SIZE = 2;
		final int NB_THREADS = 5;

		final Timer timer = new Timer();
		timer.schedule(samplingTask, 1, 1);     // chaque milliseconde...
		try {
			while (!started.getValue()) {
				Thread.sleep(15);
			}

			final List<Integer> data = new ArrayList<>(TOTAL);
			for (int i = 0 ; i < TOTAL ; ++ i) {
				data.add(i);
			}

			final ParallelBatchTransactionTemplate<Integer, BatchResults> template = new ParallelBatchTransactionTemplate<>(data, BATCH_SIZE, NB_THREADS, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
			template.execute(callback);
		}
		finally {
			timer.cancel();
		}

		// compte tenu de la méthode de mesure, on compte sur 50% de remplissable comme seuil accepté
		final int expectedSetSize = TOTAL / BATCH_SIZE + 1;
		assertTrue(expectedSetSize > 10);
		if (collector.size() > expectedSetSize || collector.size() < expectedSetSize * 5 / 10) {
			fail("Expected size of " + expectedSetSize + ", got " + collector.size() + " (" + Arrays.toString(collector.toArray(new Integer[collector.size()])) + ")");
		}
	}
}
