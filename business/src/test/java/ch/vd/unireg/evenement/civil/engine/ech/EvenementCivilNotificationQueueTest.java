package ch.vd.unireg.evenement.civil.engine.ech;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilNotificationQueueTest extends BusinessTest {

	private QueueTemplate queueTemplate;

	private static class QueueTemplate {

		private final EvenementCivilEchService serviceCivil;
		private final PlatformTransactionManager transactionManager;

		private QueueTemplate(EvenementCivilEchService evtCivilEchService, PlatformTransactionManager transactionManager) {
			this.serviceCivil = evtCivilEchService;
			this.transactionManager = transactionManager;
		}

		public void doWithNewQueueDelayedBy(int delayedBy, Callback cb) throws Exception {
			final EvenementCivilNotificationQueueImpl queue = new EvenementCivilNotificationQueueImpl(delayedBy);
			queue.setTransactionManager(transactionManager);
			queue.setEvtCivilService(serviceCivil);
			try {
				queue.afterPropertiesSet();
				cb.execute(queue);
			}
			finally {
				queue.destroy();
			}
		}

		public interface Callback {
			void execute(EvenementCivilNotificationQueue queue) throws InterruptedException;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilNotificationQueueTest.class);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final EvenementCivilEchService evtCivilEchService = getBean(EvenementCivilEchService.class, "evtCivilEchService");
		queueTemplate = new QueueTemplate(evtCivilEchService, transactionManager);
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

	@Test(timeout = 10000L)
	public void testRecupVide() throws Exception {
		queueTemplate.doWithNewQueueDelayedBy(0, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			Assert.assertNull(queue.poll(Duration.ofMillis(1)));
			Assert.assertNull(queue.poll(Duration.ofMillis(20)));
		});
	}

	@Test(timeout = 10000L)
	public void testRecupSimple() throws Exception {

		final long noIndividu = 243523L;
		final long noIndividuSans = 2433L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR);
			addEvenementCivil(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			addEvenementCivil(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
			addEvenementCivil(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE);
			addEvenementCivil(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_VERIFIER);
			addEvenementCivil(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.FORCE);
			addEvenementCivil(8L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
			addEvenementCivil(7L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
			return null;
		});

		// envois dans la queue (avec une seconde de délai afin d'être certain de pouvoir compter les éléments insérés avant qu'ils passent dans la queue finale)
		queueTemplate.doWithNewQueueDelayedBy(1, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividuSans, EvenementCivilEchProcessingMode.BATCH);
			Assert.assertEquals(1, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);
			Assert.assertEquals(2, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);      // c'est un doublon -> il ne devrait apparaître qu'une fois en sortie
			Assert.assertEquals(2, queue.getTotalCount());

			// première récupération : individu sans événement -> collection vide (timeout > 1s pour laisser le délai s'écouler, voir plus haut)
			final EvenementCivilNotificationQueue.Batch infoSans = queue.poll(Duration.ofMillis(1500));
			Assert.assertNotNull(infoSans);
			Assert.assertEquals(noIndividuSans, infoSans.noIndividu);
			Assert.assertNotNull(infoSans.contenu);
			Assert.assertEquals(0, infoSans.contenu.size());
			Assert.assertEquals(1, queue.getTotalCount());

			// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
			final EvenementCivilNotificationQueue.Batch infoAvec = queue.poll(Duration.ofMillis(1));
			Assert.assertNotNull(infoAvec);
			Assert.assertEquals(noIndividu, infoAvec.noIndividu);
			Assert.assertNotNull(infoAvec.contenu);
			Assert.assertEquals(5, infoAvec.contenu.size());
			Assert.assertEquals(0, queue.getTotalCount());
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
			Assert.assertNull(queue.poll(Duration.ofMillis(1)));
		});
	}

	@Test(timeout = 10000L)
	public void testRecupSimpleAvecPostAll() throws Exception {

		final long noIndividu = 243523L;
		final long noIndividuSans = 2433L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR);
			addEvenementCivil(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			addEvenementCivil(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
			addEvenementCivil(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE);
			addEvenementCivil(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_VERIFIER);
			addEvenementCivil(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.FORCE);
			addEvenementCivil(8L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
			addEvenementCivil(7L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
			return null;
		});

		// envois dans la queue (avec une seconde de délai afin d'être certain de pouvoir compter les éléments insérés avant qu'ils passent dans la queue finale)
		queueTemplate.doWithNewQueueDelayedBy(1, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.postAll(Arrays.asList(noIndividuSans, noIndividu, noIndividu));
			Assert.assertEquals(2, queue.getTotalCount());

			// première récupération : individu sans événement -> collection vide (timeout > 1s pour laisser le délai s'écouler, voir plus haut)
			final EvenementCivilNotificationQueue.Batch infoSans = queue.poll(Duration.ofMillis(1500));
			Assert.assertNotNull(infoSans);
			Assert.assertEquals(noIndividuSans, infoSans.noIndividu);
			Assert.assertNotNull(infoSans.contenu);
			Assert.assertEquals(0, infoSans.contenu.size());
			Assert.assertEquals(1, queue.getTotalCount());

			// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
			final EvenementCivilNotificationQueue.Batch infoAvec = queue.poll(Duration.ofMillis(1));
			Assert.assertNotNull(infoAvec);
			Assert.assertEquals(noIndividu, infoAvec.noIndividu);
			Assert.assertNotNull(infoAvec.contenu);
			Assert.assertEquals(5, infoAvec.contenu.size());
			Assert.assertEquals(0, queue.getTotalCount());
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
			Assert.assertNull(queue.poll(Duration.ofMillis(1)));
		});

	}

	@Test(timeout = 10000L)
	public void testDelay() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		queueTemplate.doWithNewQueueDelayedBy(1, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);

			// après 800ms, on ne devrait toujours rien voir
			Thread.sleep(800);
			final EvenementCivilNotificationQueue.Batch infoVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNull(infoVide);

			// mais 300ms après, alors là oui (puisque le délai est d'une seconde)
			Thread.sleep(300);
			final EvenementCivilNotificationQueue.Batch infoNonVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNotNull(infoNonVide);
			Assert.assertEquals(noIndividu, infoNonVide.noIndividu);
		});
	}

	@Test(timeout = 10000L)
	public void testNoDelay() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		queueTemplate.doWithNewQueueDelayedBy(0, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);

			// Pas de délai -> on doit donc tout de suite récupérer notre individu
			final EvenementCivilNotificationQueue.Batch info = queue.poll(Duration.ofMillis(10));
			Assert.assertNotNull(info);
			Assert.assertEquals(noIndividu, info.noIndividu);
		});
	}

	@Test(timeout = 1000L)
	public void testImmediate() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		// si on attend le délai, on fait exploser le timeout du test
		queueTemplate.doWithNewQueueDelayedBy(2, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.IMMEDIATE);

			// le poll doit recevoir l'événement immédiatement
			final EvenementCivilNotificationQueue.Batch info = queue.poll(Duration.ofMillis(10));
			Assert.assertNotNull(info);
			Assert.assertEquals(noIndividu, info.noIndividu);
		});
	}

	@Test(timeout = 10000L)
	public void testPostManual() throws Exception {

		final long noIndividuBase = 100000L;
		final int nbEvtsBatch = 500;
		final long noIndividuTemoin = 99999L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			long l = 1;
			for (; l <= nbEvtsBatch; l++) {
				addEvenementCivil(l, noIndividuBase + l, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			}
			addEvenementCivil(l + 1, noIndividuTemoin, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		queueTemplate.doWithNewQueueDelayedBy(0, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			long l = 1;
			for (; l < nbEvtsBatch; l++) {
				queue.post(noIndividuBase + l, EvenementCivilEchProcessingMode.BATCH);
			}
			queue.post(noIndividuTemoin, EvenementCivilEchProcessingMode.MANUAL);

			for (l = 1; l < nbEvtsBatch; l++) {
				final EvenementCivilNotificationQueue.Batch info = queue.poll(Duration.ofMillis(1));
				Assert.assertNotNull(info);
				if (info.noIndividu == noIndividuTemoin) {
					LOGGER.info("Témoin sorti en position " + l);
					return;
				}
			}
			Assert.fail("L'individu Témoin devrait sortir avant la fin du traitement de la totalité des evts batch");
		});
	}

	@Test(timeout = 10000L)
	public void testImmediateDejaPresent() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		queueTemplate.doWithNewQueueDelayedBy(1, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.MANUAL);
			queue.post(noIndividu, EvenementCivilEchProcessingMode.IMMEDIATE);           // post en immédiat alors que l'individu est déjà dans la queue -> ne devrait pas avoir d'effet sur le délai de traitement

			// après 800ms, on ne devrait toujours rien voir
			Thread.sleep(800);
			final EvenementCivilNotificationQueue.Batch infoVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNull(infoVide);

			// mais 300ms après, alors là oui (puisque le délai est d'une seconde)
			Thread.sleep(300);
			final EvenementCivilNotificationQueue.Batch infoNonVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNotNull(infoNonVide);
			Assert.assertEquals(noIndividu, infoNonVide.noIndividu);
		});
	}

	@Test(timeout = 20000L)
	public void testPerfInsertionWithZeroDelay() throws Exception {
		doPerfTest(0);
	}

	@Test(timeout = 30000L)
	public void testPerfInsertionWithThreeSecondsDelay() throws Exception {
		doPerfTest(3);
	}

	private void doPerfTest(int delayInSeconds) throws Exception {

		// on ne cherche pour le moment qu'à mesurer les problèmes de contention, pas la peine de s'embêter avec une dépendance vers le service des événements civils...
		final EvenementCivilNotificationQueueImpl queue = new EvenementCivilNotificationQueueImpl(delayInSeconds) {
			@Override
			protected List<EvenementCivilEchBasicInfo> buildLotsEvenementsCivils(long noIndividu) {
				return null;
			}
		};
		queue.afterPropertiesSet();
		try {

			final int nbIndividus = 10000;
			final List<Long> nos = new ArrayList<>(nbIndividus);
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

					final List<Long> random = new ArrayList<>(nos);
					Collections.shuffle(random);
					final long start = System.nanoTime();
					for (Long no : random) {
						queue.post(no, EvenementCivilEchProcessingMode.BATCH);
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
							queue.poll(Duration.ofSeconds(1));
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
			final List<Thread> feedingThreads = new ArrayList<>(nbFeedingThreads);
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
			while (queue.getTotalCount() != 0) {
				Thread.sleep(100);
			}

			pollingThread.stopIt();
			pollingThread.join();

			final long endPolling = System.nanoTime();
			LOGGER.info(String.format("Polling ended after %dms", TimeUnit.NANOSECONDS.toMillis(endPolling - start)));
		}
		finally {
			queue.destroy();
		}
	}

	/**
	 * Ce test vérifie que les threads de traitement s'arrêtent gracieusement lorsqu'ils sont interrompus de l'extérieur.
	 */
	@Test(timeout = 2000L)
	public void testQueueInterruptability() throws Exception {

		// un thread qui va demander un traitement très long
		Thread queueThread = new Thread(() -> {
			try {
				queueTemplate.doWithNewQueueDelayedBy(0, queue -> {
					Thread.sleep(3000); // fait échouer le test en timeout si va au bout du sleep
				});
			}
			catch (InterruptedException ignored) {
				// On ignore c'est dans le deroulement normal du test
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		queueThread.start();

		try {
			Thread.sleep(500); // Temporisation pour être sure que les threads de traitement soient démarré

			// on récupère les threads de traitement
			final List<Thread> threads = new ArrayList<>();
			for (Thread t : Thread.getAllStackTraces().keySet()) {
				if (t.getName().startsWith("EvtCivilEchMixer-")) {
					threads.add(t);
				}
			}

			// on les interrompt
			for (Thread t : threads) {
				t.interrupt();
			}

			// on attend qu'ils se terminent gracieusement. S'ils ne le font pas, le timeout général du test se déclenchera et le test sera en erreur.
			for (Thread t : threads) {
				t.join();
			}
		}
		finally {
			// on interrompt le traitement très long avant que le timeout général du test ne se déclenche
			queueThread.interrupt();
			queueThread.join();
		}
	}

	@Test(timeout = 2000L)
	public void testPostImmediatEtPresenceSurAutreQueueInput() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		queueTemplate.doWithNewQueueDelayedBy(1, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);
			queue.post(noIndividu, EvenementCivilEchProcessingMode.IMMEDIATE);           // post en immédiat alors que l'individu est déjà dans la queue (mais de l'autre côté) -> ne devrait pas avoir d'effet sur le délai de traitement

			// après 800ms, on ne devrait toujours rien voir
			Thread.sleep(800);
			final EvenementCivilNotificationQueue.Batch infoVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNull(infoVide);

			// mais 300ms après, alors là oui (puisque le délai est d'une seconde)
			Thread.sleep(300);
			final EvenementCivilNotificationQueue.Batch infoNonVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNotNull(infoNonVide);
			Assert.assertEquals(noIndividu, infoNonVide.noIndividu);
		});
	}

	@Test(timeout = 2000L)
	public void testDoublonSurQueuesSeparees() throws Exception {

		final long noIndividu = 243523L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(status -> {
			addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
			return null;
		});

		// envois dans la queue
		queueTemplate.doWithNewQueueDelayedBy(1, queue -> {
			Assert.assertEquals(0, queue.getTotalCount());
			queue.post(noIndividu, EvenementCivilEchProcessingMode.BATCH);
			queue.post(noIndividu, EvenementCivilEchProcessingMode.MANUAL);

			// après 800ms, on ne devrait toujours rien voir
			Thread.sleep(800);
			final EvenementCivilNotificationQueue.Batch infoVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNull(infoVide);

			// mais 300ms après, alors là oui (puisque le délai est d'une seconde)
			Thread.sleep(300);
			final EvenementCivilNotificationQueue.Batch infoNonVide = queue.poll(Duration.ofMillis(1));
			Assert.assertNotNull(infoNonVide);
			Assert.assertEquals(noIndividu, infoNonVide.noIndividu);

			// mais pas une seconde fois (= doublon bien détecté)
			final EvenementCivilNotificationQueue.Batch infoVide2 = queue.poll(Duration.ofMillis(1));
			Assert.assertNull(infoVide2);
		});
	}
}
