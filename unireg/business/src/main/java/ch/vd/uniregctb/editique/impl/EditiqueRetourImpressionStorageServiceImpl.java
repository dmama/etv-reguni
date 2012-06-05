package ch.vd.uniregctb.editique.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatRecu;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;
import ch.vd.uniregctb.editique.RetourImpressionTrigger;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation du service de stockage des retours d'impression de l'éditique
 */
public class EditiqueRetourImpressionStorageServiceImpl implements EditiqueRetourImpressionStorageService, InitializingBean, DisposableBean {

	public static final Logger LOGGER = Logger.getLogger(EditiqueRetourImpressionStorageServiceImpl.class);

	private static final String SERVICE_NAME = "ImpressionLocale";

	private StatsService statsService;

	private final ServiceTracing serviceTracing = new ServiceTracing(SERVICE_NAME);

	/**
	 * Map des impressions reçues et pas encore réclamées
	 */
	private final Map<String, EditiqueResultatRecu> impressionsRecues = new HashMap<String, EditiqueResultatRecu>();

	/**
	 * Map des triggers enregistrés pour être déclenchés à la réception de nouveaux retours d'impression
	 * (le Long est le timestamp donné par {@link System#nanoTime()} au moment de l'enregistrement du trigger)
	 */
	private final Map<String, Pair<Long, RetourImpressionTrigger>> delayedTriggers = new HashMap<String, Pair<Long, RetourImpressionTrigger>>();

	/**
	 * Thread de surveillance du contenu de la map des impressions reçues (voir {@link #impressionsRecues}) et qui
	 * déclenche l'éventuel trigger enregistré associé aux documents présents dans la map en question
	 */
	private TriggerThread triggerManagerThread;

	/**
	 * Timer de nettoyage des impressions reçues et non réclamées
	 */
	private Timer cleanupTimer = null;

	/**
	 * Tâche de nettoyage des impressions reçues et non réclamées, associée au timer {@link #cleanupTimer}
	 */
	private TimerTask cleanupTask = null;

	/**
	 * Date de la dernière purge (= nettoyage avéré) d'une impression reçue et non réclamée
	 */
	private Date dateDernierePurgeEffective = null;

	/**
	 * Compteur du nombre de documents d'impression effectivement purgés par la tâche de nettoyage {@link #cleanupTask} depuis le démarrage du service
	 */
	private int purgedDocuments = 0;

	/**
	 * Compteur du nombre de documents d'impression reçus depuis le démarrage du service
	 */
	private int receivedDocuments = 0;

	/**
	 * Période du timer du cleanup (secondes) : à chaque tick, on va enlever de la map des impressions
	 * reçues les données qui étaient déjà là au tick précédent
	 */
	private int cleanupPeriod;

	@Override
	public void setCleanupPeriod(int cleanupPeriod) {
		if (cleanupPeriod <= 0) {
			throw new IllegalArgumentException("La valeur doit être strictement positive");
		}
		if (this.cleanupPeriod != cleanupPeriod) {
			this.cleanupPeriod = cleanupPeriod;
			restartCleanupTimer();

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Le délai de purge des documents imprimés non réclamés est de %d seconde%s.", cleanupPeriod, cleanupPeriod > 1 ? "s" : ""));
			}
		}
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	private void restartCleanupTimer() {
		if (cleanupTimer != null) {
			if (cleanupTask != null) {
				cleanupTask.cancel();
			}
			cleanupTask = new CleanupTask();
			cleanupTimer.schedule(cleanupTask, cleanupPeriod * 1000L, cleanupPeriod * 1000L);
		}
	}

	@Override
	public int getCleanupPeriod() {
		return cleanupPeriod;
	}

	/**
	 * Tâche de cleanup des vieux retours d'impressions que personne n'a demandé
	 */
	private final class CleanupTask extends TimerTask {

		@Override
		public void run() {
			final long tickPrecedent = TimeHelper.getPreciseCurrentTimeMillis() - cleanupPeriod * 1000L;
			synchronized (impressionsRecues) {
				final Iterator<Map.Entry<String, EditiqueResultatRecu>> iterator = impressionsRecues.entrySet().iterator();
				while (iterator.hasNext()) {
					final Map.Entry<String, EditiqueResultatRecu> entry = iterator.next();
					final EditiqueResultatRecu retour = entry.getValue();
					if (retour.getTimestampReceived() < tickPrecedent) {
						LOGGER.warn(String.format("Cleanup du retour d'impression '%s' qui n'intéresse apparemment personne", retour.getIdDocument()));
						iterator.remove();

						++ purgedDocuments;
						dateDernierePurgeEffective = new Date();
					}
				}
			}
		}
	}

	private final class TriggerThread extends Thread {

		private boolean stopping = false;

		public TriggerThread() {
			super("RetourImpressionTrigger");
		}

		@Override
		public void run() {

			LOGGER.info(String.format("Démarrage du thread %s", getName()));
			try {
				// on attend les arrivées des nouvelles impressions
				synchronized (impressionsRecues) {
					while (!stopping) {
						final Iterator<Map.Entry<String, EditiqueResultatRecu>> iterator = impressionsRecues.entrySet().iterator();
						while (iterator.hasNext()) {
							final Map.Entry<String, EditiqueResultatRecu> entry = iterator.next();
							final Pair<Long, RetourImpressionTrigger> trigger = delayedTriggers.remove(entry.getKey());
							if (trigger != null) {
								iterator.remove();
								final EditiqueResultatRecu resultat = entry.getValue();
								try {
									if (LOGGER.isDebugEnabled()) {
										final long now = System.nanoTime();
										final String duration = TimeHelper.formatDuree(TimeUnit.NANOSECONDS.toMillis(now - trigger.getFirst()));
										LOGGER.debug(String.format("Exécution du trigger enregistré pour le document '%s' il y a %s", resultat.getIdDocument(), duration));
									}
									trigger.getSecond().trigger(resultat);
								}
								catch (Throwable e) {
									LOGGER.error(String.format("Exception levée lors du traitement du document '%s' par le trigger associé", resultat.getIdDocument()), e);
								}
							}
						}

						impressionsRecues.wait();
					}
				}
			}
			catch (InterruptedException e) {
				LOGGER.warn("Le thread des triggers des impressions reçues a été interrompu", e);
			}
			finally {
				LOGGER.info(String.format("Arrêt du thread %s", getName()));
			}
		}

		public void stopIt() {
			stopping = true;
			synchronized (impressionsRecues) {
				// tout le monde debout !
				impressionsRecues.notifyAll();
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (cleanupPeriod <= 0) {
			throw new IllegalArgumentException("La valeur de 'cleanupPeriod' doit être strictement positive");
		}
		cleanupTimer = new Timer("RetourImpressionCleanup");
		restartCleanupTimer();
		statsService.registerService(SERVICE_NAME, serviceTracing);

		triggerManagerThread = new TriggerThread();
		triggerManagerThread.start();
	}

	@Override
	public void destroy() throws Exception {
		triggerManagerThread.stopIt();

		cleanupTimer.cancel();
		cleanupTimer = null;
		triggerManagerThread.join();
		triggerManagerThread = null;

		statsService.unregisterService(SERVICE_NAME);
	}

	/**
	 * Enregistre un trigger qui sera déclenché à la réception du retour d'impression identifié par son ID
	 * @param nomDocument ID du document déclencheur
	 * @param trigger action à lancer à la réception du document voulu
	 */
	@Override
	public void registerTrigger(String nomDocument, RetourImpressionTrigger trigger) {

		synchronized (impressionsRecues) {

			// on enregistre le trigger ...
			delayedTriggers.put(nomDocument, new Pair<Long, RetourImpressionTrigger>(System.nanoTime(), trigger));

			// .., et on réveille tout le monde : si le document
			// est en fait déjà là, il sera alors traité par le réveil
			impressionsRecues.notifyAll();
		}
	}

	@Override
	public void onArriveeRetourImpression(EditiqueResultatRecu resultat) {

		// ah ? un retour d'impression ? il faut le mettre dans la map
		// et dire à tous ceux qui attendent qu'il y a du nouveau...

		synchronized (impressionsRecues) {

			final String nomDocument = resultat.getIdDocument();
			impressionsRecues.put(nomDocument, resultat);

			++ receivedDocuments;

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Réception du document imprimé '%s'", nomDocument));
			}

			impressionsRecues.notifyAll();
		}
	}

	@Override
	public EditiqueResultat getDocument(String nomDocument, long timeout) {

		Assert.isTrue(timeout > 0);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Demande de récupération du document '%s'", nomDocument));
		}

		final long start = serviceTracing.start();
		try {
			final long tsAttente = TimeHelper.getPreciseCurrentTimeMillis() + timeout;        // on n'attendra pas plus tard...

			synchronized (impressionsRecues) {

				// on attends le temps qu'il faut...
				while (true) {

					// déjà là ?
					final EditiqueResultat resultat = impressionsRecues.remove(nomDocument);
					if (resultat == null) {

						// et non, on attends un peu... mais pas trop quand-même !
						final long tempsRestant = tsAttente - TimeHelper.getPreciseCurrentTimeMillis();
						if (tempsRestant <= 0) {

							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug(String.format("Timeout dépassé pour la récupération du document '%s'", nomDocument));
							}

							return new EditiqueResultatTimeoutImpl(nomDocument);
						}

						try {
							impressionsRecues.wait(tempsRestant);
						}
						catch (InterruptedException e) {
							// interrompu...? on s'en va
							return new EditiqueResultatTimeoutImpl(nomDocument);
						}
					}
					else {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(String.format("Document '%s' trouvé", nomDocument));
						}
						return resultat;
					}
				}
			}
		}
		finally {
			serviceTracing.end(start, "getDocument", nomDocument);
		}
	}

	@Override
	public int getDocumentsEnAttenteDeDispatch() {
		synchronized (impressionsRecues) {
			return impressionsRecues.size();
		}
	}

	@Override
	public int getDocumentsPurges() {
		return purgedDocuments;
	}

	@Override
	public Date getDateDernierePurgeEffective() {
		return dateDernierePurgeEffective;
	}

	@Override
	public int getDocumentsRecus() {
		return receivedDocuments;
	}

	@Override
	public Collection<Pair<Long, RetourImpressionTrigger>> getTriggersEnregistres() {
		synchronized(impressionsRecues) {
			return new ArrayList<Pair<Long, RetourImpressionTrigger>>(delayedTriggers.values());
		}
	}
}
