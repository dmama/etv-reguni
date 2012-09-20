package ch.vd.uniregctb.evenement.civil.engine.regpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;

public class EvenementCivilAsyncProcessorTest extends BusinessTest {

	public static final Logger LOGGER = Logger.getLogger(EvenementCivilAsyncProcessorTest.class);

	private EvenementCivilAsyncProcessorImpl asyncProcessor;

	/**
	 * Classe anonyme pour vérifier ce qui est finalement transmis au
	 * processeur des événements civils
	 */
	private static class MyProcessor implements EvenementCivilProcessor {

		/**
		 * Cette collection est synchronisée afin que les appels à traiteEvenementCivil et la lecture
		 * des éléments de la collection (principalement la taille de la collection) ne se marchent pas dessus
		 */
		private final List<Long> evenementsRecus = Collections.synchronizedList(new ArrayList<Long>());

		@Override
		public void traiteEvenementsCivils(StatusManager status) {
			throw new NotImplementedException("Méthode non implémentée, normalement inutile pour le test!");
		}

		@Override
		public void traiteEvenementCivil(Long id) {
			evenementsRecus.add(id);
		}

		@Override
		public void recycleEvenementCivil(Long id) {
			throw new NotImplementedException("Méthode non implémentée, normalement inutile pour le test!");
		}

		@Override
		public void forceEvenementCivil(EvenementCivilRegPP evenementCivilExterne) {
			throw new NotImplementedException("Méthode non implémentée, normalement inutile pour le test!");
		}

		public List<Long> getEvenementsRecus() {
			return evenementsRecus;
		}
	}

	@Override
	public void onTearDown() throws Exception {
		if (asyncProcessor != null) {
			asyncProcessor.stop();
			asyncProcessor = null;
		}

		super.onTearDown();
	}

	private void buildAsyncProcessor(EvenementCivilProcessor processor, int delaiPriseEnCompte) throws Exception {

		asyncProcessor = new EvenementCivilAsyncProcessorImpl();

		// cette valeur (en secondes) suppose que tous les événements civils qui doivent être triés (ils arrivent ici dans n'importe quel ordre)
		// sont postés dans ce laps de temps
		asyncProcessor.setDelaiPriseEnCompte(delaiPriseEnCompte);

		final EvenementCivilRegPPDAO evtCivilExterneDAO = getBean(EvenementCivilRegPPDAO.class, "evenementCivilRegPPDAO");
		asyncProcessor.setEvenementCivilRegPPDAO(evtCivilExterneDAO);
		asyncProcessor.setEvenementCivilProcessor(processor);
		asyncProcessor.setHibernateTemplate(hibernateTemplate);
		asyncProcessor.setTransactionManager(transactionManager);
		asyncProcessor.setFetchAwaitingEventsOnStart(false);
		asyncProcessor.afterPropertiesSet();
		asyncProcessor.start();
	}

	@Test(timeout=10000)
	@Transactional(rollbackFor = Throwable.class)
	public void testOrdreTraitement() throws Exception {

		final MyProcessor processor = new MyProcessor();
		buildAsyncProcessor(processor, 1);

		final int THREADS = 10;
		final int EVTS_PAR_THREAD = 100;

		// on met en place une dizaine de threads qui envoient chacun plusieurs événements civils
		// -> quel que soit l'ordre d'envoi des événements civils, ils doivent arriver dans l'ordre
		final List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0 ; i < THREADS; ++ i) {
			final int threadIndex = i;
			final Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int j = EVTS_PAR_THREAD - 1 ; j >= 0 ; -- j) {
						final long id = threadIndex * EVTS_PAR_THREAD + j;
						asyncProcessor.postEvenementCivil(id);
					}
				}
			}, String.format("Thread-%d", threadIndex));

			threads.add(thread);
		}

		final long start = System.nanoTime();

		// nouvelle boucle pour lancer les threads
		for (Thread thread : threads) {
			thread.start();
		}

		// nouvelle boucle pour attendre que tous les threads (de postage) ont terminé
		for (Thread thread : threads) {
			thread.join();
		}

		final long finEnvoi = System.nanoTime();
		LOGGER.warn(String.format("Envoyé %d événements avec %d threads en %d ms (le test peut échouer si ce délai est plus grand qu'une seconde)", EVTS_PAR_THREAD * THREADS, THREADS, (finEnvoi - start) / 1000000L));

		// on attends que tous les événements soient passés (= que la queue les dispatche)
		asyncProcessor.sync();

		// maintenant on regarde ce qui est passé et dans quel ordre...
		final List<Long> evtsRecus = processor.getEvenementsRecus();
		Assert.assertEquals(EVTS_PAR_THREAD * THREADS, evtsRecus.size());

		final long finTraitement = System.nanoTime();
		LOGGER.warn(String.format("Pseudo-traitement des %d événements en %d ms (depuis le départ)", EVTS_PAR_THREAD * THREADS, (finTraitement - start) / 1000000L));

		// les événements doivent tous être passés dans l'ordre
		for (int index = 0 ; index < evtsRecus.size() ; ++ index) {
			Assert.assertEquals(index, (long) evtsRecus.get(index));
		}
	}

	/**
	 * Test de non-régression qui vérifie que le sync fonctionne bien comme
	 * on s'y attend (une ancienne implémentation faisait échouer ce test)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSyncOnOneElement() throws Exception {
		final MyProcessor processor = new MyProcessor();
		buildAsyncProcessor(processor, 1);
		Thread.sleep(100);      // pour laisser le temps au thread d'écoute sur la queue de démarrer

		LOGGER.info("Post de l'événement civil");
		asyncProcessor.postEvenementCivil(1);

		Thread.sleep(1);        // pour laisser le temps au thread d'écoute sur la queue de récupérer l'élément sur la queue

		asyncProcessor.sync();
		LOGGER.info("Sync terminé");
		Assert.assertEquals(1, processor.getEvenementsRecus().size());
	}
}
