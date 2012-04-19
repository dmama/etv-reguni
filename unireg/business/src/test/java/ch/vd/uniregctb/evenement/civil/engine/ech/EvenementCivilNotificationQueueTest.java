package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilNotificationQueueTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilNotificationQueueTest.class);

	private EvenementCivilNotificationQueueImpl buildQueue(int delayInSeconds) {
		final EvenementCivilNotificationQueueImpl queue = new EvenementCivilNotificationQueueImpl(delayInSeconds);
		queue.setEvtCivilService(getBean(EvenementCivilEchService.class, "evtCivilEchService"));
		return queue;
	}

	private EvenementCivilEch addEvenementCivil(Long id, long noIndividu, RegDate date, TypeEvenementCivilEch type, ActionEvenementCivilEch action, EtatEvenementCivil etat) {
		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(id);
		evt.setDateEvenement(date);
		evt.setNumeroIndividu(noIndividu);
		evt.setType(type);
		evt.setAction(action);
		evt.setEtat(etat);
		return hibernateTemplate.merge(evt);
	}

	@Test
	public void testRecupVide() throws Exception {
		final EvenementCivilNotificationQueueImpl queue = buildQueue(0);
		Assert.assertEquals(0, queue.getInflightCount());
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
		Assert.assertNull(queue.poll(20, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testRecupSimple() throws Exception {

		final long noIndividu = 243523L;
		final long noIndividuSans = 2433L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR);
				addEvenementCivil(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				addEvenementCivil(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE);
				addEvenementCivil(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_VERIFIER);
				addEvenementCivil(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.FORCE);
				addEvenementCivil(8L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(7L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				return null;
			}
		});

		// envois dans la queue
		final EvenementCivilNotificationQueueImpl queue = buildQueue(0);
		Assert.assertEquals(0, queue.getInflightCount());
		queue.post(noIndividuSans, false);
		Assert.assertEquals(1, queue.getInflightCount());
		queue.post(noIndividu, false);
		Assert.assertEquals(2, queue.getInflightCount());
		queue.post(noIndividu, false);      // c'est un doublon -> il ne devrait apparaître qu'une fois en sortie
		Assert.assertEquals(2, queue.getInflightCount());

		// première récupération : individu sans événement -> collection vide
		final EvenementCivilNotificationQueue.Batch infoSans = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoSans);
		Assert.assertEquals(noIndividuSans, infoSans.noIndividu);
		Assert.assertNotNull(infoSans.contenu);
		Assert.assertEquals(0, infoSans.contenu.size());
		Assert.assertEquals(1, queue.getInflightCount());

		// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
		final EvenementCivilNotificationQueue.Batch infoAvec = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoAvec);
		Assert.assertEquals(noIndividu, infoAvec.noIndividu);
		Assert.assertNotNull(infoAvec.contenu);
		Assert.assertEquals(5, infoAvec.contenu.size());
		Assert.assertEquals(0, queue.getInflightCount());
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(0);
			Assert.assertEquals(1L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 1, 1), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(1);
			Assert.assertEquals(5L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 2, 5), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.A_TRAITER, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(2);
			Assert.assertEquals(3L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(3);
			Assert.assertEquals(7L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.ARRIVEE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(4);
			Assert.assertEquals(8L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.DIVORCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}

		// troisième tentative de récupération : rien
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testRecupSimpleAvecPostAll() throws Exception {

		final long noIndividu = 243523L;
		final long noIndividuSans = 2433L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR);
				addEvenementCivil(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				addEvenementCivil(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE);
				addEvenementCivil(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_VERIFIER);
				addEvenementCivil(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.FORCE);
				addEvenementCivil(8L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(7L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				return null;
			}
		});

		// envois dans la queue
		final EvenementCivilNotificationQueueImpl queue = buildQueue(0);
		Assert.assertEquals(0, queue.getInflightCount());
		queue.postAll(Arrays.asList(noIndividuSans, noIndividu, noIndividu));
		Assert.assertEquals(2, queue.getInflightCount());

		// première récupération : individu sans événement -> collection vide
		final EvenementCivilNotificationQueue.Batch infoSans = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoSans);
		Assert.assertEquals(noIndividuSans, infoSans.noIndividu);
		Assert.assertNotNull(infoSans.contenu);
		Assert.assertEquals(0, infoSans.contenu.size());
		Assert.assertEquals(1, queue.getInflightCount());

		// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
		final EvenementCivilNotificationQueue.Batch infoAvec = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoAvec);
		Assert.assertEquals(noIndividu, infoAvec.noIndividu);
		Assert.assertNotNull(infoAvec.contenu);
		Assert.assertEquals(5, infoAvec.contenu.size());
		Assert.assertEquals(0, queue.getInflightCount());
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(0);
			Assert.assertEquals(1L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 1, 1), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(1);
			Assert.assertEquals(5L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 2, 5), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.A_TRAITER, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(2);
			Assert.assertEquals(3L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(3);
			Assert.assertEquals(7L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.ARRIVEE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(4);
			Assert.assertEquals(8L, evtCivilInfo.getId());
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.getDate());
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.getEtat());
			Assert.assertEquals(TypeEvenementCivilEch.DIVORCE, evtCivilInfo.getType());
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.getAction());
		}

		// troisième tentative de récupération : rien
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testDelay() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				return null;
			}
		});

		// envois dans la queue
		final EvenementCivilNotificationQueueImpl queue = buildQueue(1);        // 1 seconde
		Assert.assertEquals(0, queue.getInflightCount());
		queue.post(noIndividu, false);

		// après 800ms, on ne devrait toujours rien voir
		Thread.sleep(800);
		final EvenementCivilNotificationQueue.Batch infoVide = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNull(infoVide);

		// mais 300ms après, alors là oui (puisque le délai est d'une seconde)
		Thread.sleep(300);
		final EvenementCivilNotificationQueue.Batch infoNonVide = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoNonVide);
		Assert.assertEquals(noIndividu, infoNonVide.noIndividu);
	}

	@Test(timeout = 1000L)
	public void testImmediate() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				return null;
			}
		});

		// envois dans la queue
		final EvenementCivilNotificationQueueImpl queue = buildQueue(2);      // si on attend le délai, on fait exploser le timeout du test
		Assert.assertEquals(0, queue.getInflightCount());
		queue.post(noIndividu, true);

		// le poll doit recevoir l'événement immédiatement
		final EvenementCivilNotificationQueue.Batch info = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(info);
		Assert.assertEquals(noIndividu, info.noIndividu);
	}

	@Test
	public void testImmediateDejaPresent() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				return null;
			}
		});

		// envois dans la queue
		final EvenementCivilNotificationQueueImpl queue = buildQueue(1);        // 1 seconde
		Assert.assertEquals(0, queue.getInflightCount());
		queue.post(noIndividu, false);
		queue.post(noIndividu, true);           // post en immédiat alors que l'individu est déjà dans la queue -> ne devrait pas avoir d'effet sur le délai de traitement

		// après 800ms, on ne devrait toujours rien voir
		Thread.sleep(800);
		final EvenementCivilNotificationQueue.Batch infoVide = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNull(infoVide);

		// mais 300ms après, alors là oui (puisque le délai est d'une seconde)
		Thread.sleep(300);
		final EvenementCivilNotificationQueue.Batch infoNonVide = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoNonVide);
		Assert.assertEquals(noIndividu, infoNonVide.noIndividu);
	}

	@Test(timeout = 10000L)
	public void testPerfInsertionWithZeroDelay() throws Exception {
		doPerfTest(0);
	}

	@Test(timeout = 20000L)
	public void testPerfInsertionWithThreeSecondsDelay() throws Exception {
		doPerfTest(3);
	}

	private void doPerfTest(int delayInSeconds) throws Exception {

		// on ne cherche pour le moment qu'à mesurer les problèmes de contention, pas la peine de s'embêter avec une dépendance vers le service des événements civils...
		final EvenementCivilNotificationQueue queue = new EvenementCivilNotificationQueueImpl(delayInSeconds) {
			@Override
			protected List<EvenementCivilEchBasicInfo> buildLotsEvenementsCivils(long noIndividu) {
				return null;
			}
		};

		final int nbIndividus = 10000;
		final List<Long> nos = new ArrayList<Long>(nbIndividus);
		for (long i = 0L ; i < nbIndividus ; ++ i) {
			nos.add(i);
		}

		final class FeedingThread extends Thread {

			private final int index;

			FeedingThread(int index) {
				this.index = index;
			}

			@Override
			public void run() {
				LOGGER.info(String.format("Thread %d started", index));

				final List<Long> random = new ArrayList<Long>(nos);
				Collections.shuffle(random);
				final long start = System.nanoTime();
				for (Long no : random) {
					queue.post(no, false);
				}

				final long end = System.nanoTime();
				LOGGER.info(String.format("Thread %d finished after %dms", index, TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
		}

		final class PollingThread extends Thread {

			boolean stop = false;

			@Override
			public void run() {
				while (!stop) {
					try {
						queue.poll(1, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}

			public void stopIt() {
				stop = true;
			}
		}

		final int nbFeedingThreads = 20;
		final List<Thread> feedingThreads = new ArrayList<Thread>(nbFeedingThreads);
		for (int i = 0 ; i < nbFeedingThreads ; ++ i) {
			feedingThreads.add(new FeedingThread(i));
		}

		final PollingThread pollingThread = new PollingThread();
		pollingThread.start();

		final long start = System.nanoTime();
		LOGGER.info("Starting...");

		// start all threads
		for (Thread t : feedingThreads) {
			t.start();
		}

		// wait for all threads to finish
		for (Thread t : feedingThreads) {
			t.join();
		}

		final long endFeeding = System.nanoTime();
		LOGGER.info(String.format("Feeding ended after %dms", TimeUnit.NANOSECONDS.toMillis(endFeeding - start)));

		// wait for the queue to be empty
		while (queue.getInflightCount() != 0) {
			Thread.sleep(100);
		}

		pollingThread.stopIt();
		pollingThread.join();

		final long endPolling = System.nanoTime();
		LOGGER.info(String.format("Polling ended after %dms", TimeUnit.NANOSECONDS.toMillis(endPolling - start)));
	}
}
