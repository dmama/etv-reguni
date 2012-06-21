package ch.vd.uniregctb.editique.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.AsyncStorageWithPeriodicCleanup;
import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.editique.EditiqueResultatRecu;
import ch.vd.uniregctb.editique.RetourImpressionTrigger;

public class EditiqueRetourImpressionStorage extends AsyncStorageWithPeriodicCleanup<String, EditiqueResultatRecu> {

	private static final Logger LOGGER = Logger.getLogger(EditiqueRetourImpressionStorage.class);

	/**
	 * Thread de gestion des triggers des actions à faire si le document arrive alors que personne ne l'attend plus
	 */
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
				synchronized (map) {
					while (!stopping) {
						final Iterator<Map.Entry<String, DataHolder<EditiqueResultatRecu>>> iterator = map.entrySet().iterator();
						while (iterator.hasNext()) {
							final Map.Entry<String, DataHolder<EditiqueResultatRecu>> entry = iterator.next();
							final Pair<Long, RetourImpressionTrigger> trigger = delayedTriggers.remove(entry.getKey());
							if (trigger != null) {
								iterator.remove();
								final DataHolder<EditiqueResultatRecu> dh = entry.getValue();
								try {
									if (LOGGER.isDebugEnabled()) {
										final long now = System.nanoTime();
										final String duration = TimeHelper.formatDuree(TimeUnit.NANOSECONDS.toMillis(now - trigger.getFirst()));
										LOGGER.debug(String.format("Exécution du trigger enregistré pour le document '%s' il y a %s", dh.data.getIdDocument(), duration));
									}
									trigger.getSecond().trigger(dh.data);
								}
								catch (Throwable e) {
									LOGGER.error(String.format("Exception levée lors du traitement du document '%s' par le trigger associé", dh.data.getIdDocument()), e);
								}
							}
						}

						map.wait();
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
			synchronized (map) {
				// tout le monde debout !
				map.notifyAll();
			}
		}
	}

	/**
	 * Map des triggers enregistrés pour être déclenchés à la réception de nouveaux retours d'impression
	 * (le Long est le timestamp donné par {@link System#nanoTime()} au moment de l'enregistrement du trigger)
	 */
	private final Map<String, Pair<Long, RetourImpressionTrigger>> delayedTriggers = new HashMap<String, Pair<Long, RetourImpressionTrigger>>();

	/**
	 * Thread de surveillance du contenu de la map des impressions reçues (voir {@link #map}) et qui
	 * déclenche l'éventuel trigger enregistré associé aux documents présents dans la map en question
	 */
	private TriggerThread triggerManagerThread;

	/**
	 * Date de la dernière purge (= nettoyage avéré) d'une impression reçue et non réclamée
	 */
	private Date dateDernierePurgeEffective = null;

	/**
	 * Compteur du nombre de documents d'impression effectivement purgés par la tâche de nettoyage depuis le démarrage du service
	 */
	private final AtomicInteger purgedDocuments = new AtomicInteger(0);

	/**
	 * Constructeur
	 * @param cleanupPeriodSeconds en secondes, la période de cleanup des documents reçus non réclamés
	 */
	public EditiqueRetourImpressionStorage(int cleanupPeriodSeconds) {
		super(cleanupPeriodSeconds, "RetourImpressionCleanup");
	}

	@Override
	protected CleanupTask buildCleanupTask() {
		return new CleanupTask() {
			@Override
			protected void onPurge(String key, EditiqueResultatRecu value) {
				LOGGER.warn(String.format("Cleanup du retour d'impression '%s' qui n'intéresse apparemment personne", key));
				purgedDocuments.incrementAndGet();
				dateDernierePurgeEffective = new Date();
			}
		};
	}

	/**
	 * @return Le nombre de document purgés (reçus non réclamés depuis trop longtemps)
	 */
	public int getNbPurgedDocuments() {
		return purgedDocuments.intValue();
	}

	/**
	 * @return La date de la dernière purge ayant donné lieu à une suppression de donnée
	 */
	@Nullable
	public Date getDateDernierePurgeEffective() {
		return dateDernierePurgeEffective;
	}

	/**
	 * Démarre les threads de cleanup (classe de base) et de la gestion des triggers
	 */
	public void start() {
		Assert.isTrue(triggerManagerThread == null);
		super.start();

		triggerManagerThread = new TriggerThread();
		triggerManagerThread.start();
	}

	/**
	 * Arrête les threads de cleanup (classe de base) et de la gestion des triggers
	 */
	public void stop() throws Exception {
		if (triggerManagerThread != null) {
			triggerManagerThread.stopIt();
			triggerManagerThread.join();
			triggerManagerThread = null;
		}
		super.stop();
	}

	/**
	 * Enregistre un trigger qui sera déclenché à la réception du retour d'impression identifié par son ID
	 * @param nomDocument ID du document déclencheur
	 * @param trigger action à lancer à la réception du document voulu
	 */
	public void registerTrigger(String nomDocument, RetourImpressionTrigger trigger) {

		synchronized (map) {

			// on enregistre le trigger ...
			delayedTriggers.put(nomDocument, new Pair<Long, RetourImpressionTrigger>(System.nanoTime(), trigger));

			// .., et on réveille tout le monde : si le document
			// est en fait déjà là, il sera alors traité par le réveil
			map.notifyAll();
		}
	}

	public Collection<Pair<Long, RetourImpressionTrigger>> getTriggersEnregistres() {
		synchronized (map) {
			return new ArrayList<Pair<Long, RetourImpressionTrigger>>(delayedTriggers.values());
		}
	}
}
