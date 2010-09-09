package ch.vd.uniregctb.evenement.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilAsyncProcessorTest extends BusinessTest {

	public static final Logger LOGGER = Logger.getLogger(EvenementCivilAsyncProcessorTest.class);

	private EvenementCivilAsyncProcessorImpl asyncProcessor;
	private EvenementCivilDAO evtCivilDAO;
	private MyProcessor processor;

	/**
	 * Classe anonyme pour vérifier ce qui est finalement transmis au
	 * processeur des événements civils
	 */
	private static final class MyProcessor implements EvenementCivilProcessor {

		/**
		 * Cette collection est synchronisée afin que les appels à traiteEvenementCivil et la lecture
		 * des éléments de la collection (principalement la taille de la collection) ne se marchent pas dessus
		 */
		private final List<Long> evenementsRecus = Collections.synchronizedList(new ArrayList<Long>());

		public void traiteEvenementsCivils(StatusManager status) {
			throw new NotImplementedException("Méthode non implémentée, normalement inutile pour le test!");
		}

		public Long traiteEvenementCivil(Long id, boolean refreshCache) {
			evenementsRecus.add(id);
			return id;
		}

		public Long recycleEvenementCivil(Long id) {
			throw new NotImplementedException("Méthode non implémentée, normalement inutile pour le test!");
		}

		public List<Long> getEvenementsRecus() {
			return evenementsRecus;
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evtCivilDAO = getBean(EvenementCivilDAO.class, "evenementCivilDAO");

		processor = new MyProcessor();

		asyncProcessor = new EvenementCivilAsyncProcessorImpl();

		// cette valeur 1 (= 1 seconde) suppose que tous les événements civils qui doivent être triés (ils arrivent ici dans n'importe quel ordre)
		// sont postés dans la même seconde
		asyncProcessor.setDelaiPriseEnCompte(1);
		
		asyncProcessor.setEvenementCivilDAO(evtCivilDAO);
		asyncProcessor.setEvenementCivilProcessor(processor);
		asyncProcessor.setHibernateTemplate(hibernateTemplate);
		asyncProcessor.setTransactionManager(transactionManager);
		asyncProcessor.setFetchAwaitingEventsOnStart(false);
		asyncProcessor.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		asyncProcessor.destroy();
		asyncProcessor = null;

		super.onTearDown();
	}

	private static EvenementCivilData buildEvenement(long id, long noIndividu, RegDate dateEvenement, TypeEvenementCivil type) {
		final EvenementCivilData evt = new EvenementCivilData();
		evt.setId(id);
		evt.setDateEvenement(dateEvenement);
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setNumeroIndividuPrincipal(noIndividu);
		evt.setType(type);
		evt.setNumeroOfsCommuneAnnonce(MockCommune.Lausanne.getNoOFSEtendu());
		return evt;
	}

	@Test(timeout=10000)
	public void testOrdreTraitement() throws Exception {

		final int THREADS = 10;
		final int EVTS_PAR_THREAD = 5;

		final Object lock = new Object();

		// on met en place une dizaine de threads qui envoient chacun une dizaine d'événements civils
		// -> quel que soit l'ordre d'envoi des événements civils, ils doivent arriver dans l'ordre
		final List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0 ; i < THREADS; ++ i) {
			final int threadIndex = i;
			final Thread thread = new Thread(new Runnable() {
				public void run() {
					AuthenticationHelper.setPrincipal("Evt-Civil-Thread-" + threadIndex);
					for (int j = 0 ; j < EVTS_PAR_THREAD ; ++ j) {
						final long id = threadIndex * EVTS_PAR_THREAD + j;
						final RegDate date = RegDate.get();

						final TransactionTemplate template = new TransactionTemplate(transactionManager);
						template.execute(new TransactionCallback() {
							public Object doInTransaction(TransactionStatus status) {
								final EvenementCivilData evt = buildEvenement(id, id, date, TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS);
								evtCivilDAO.save(evt);
								asyncProcessor.postEvenementCivil(id, System.currentTimeMillis(), lock);
								return null;
							}
						});
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

		// on attends que tous les événements sont passés (= que la queue les dispatche)
		final List<Long> evtsRecus = processor.getEvenementsRecus();
		synchronized (lock) {
			while (evtsRecus.size() < EVTS_PAR_THREAD * THREADS) {
				lock.wait();
			}
		}
		Assert.assertEquals(EVTS_PAR_THREAD * THREADS, evtsRecus.size());

		final long finTraitement = System.nanoTime();
		LOGGER.warn(String.format("Pseudo-traitement des %d événements en %d ms (depuis le départ)", EVTS_PAR_THREAD * THREADS, (finTraitement - start) / 1000000L));

		// les événements doivent tous être passés dans l'ordre
		for (int index = 0 ; index < evtsRecus.size() ; ++ index) {
			Assert.assertEquals(index, (long) evtsRecus.get(index));
		}
	}
}
