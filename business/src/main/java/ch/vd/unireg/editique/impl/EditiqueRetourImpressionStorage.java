package ch.vd.unireg.editique.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.AsyncStorageWithPeriodicCleanup;
import ch.vd.unireg.common.TimeHelper;
import ch.vd.unireg.editique.EditiqueResultatRecu;
import ch.vd.unireg.editique.RetourImpressionTrigger;

public class EditiqueRetourImpressionStorage extends AsyncStorageWithPeriodicCleanup<String, EditiqueResultatRecu> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EditiqueRetourImpressionStorage.class);

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
				doInLockedEnvironment(new Action<String, EditiqueResultatRecu, Object>() {
					@Override
					public Object execute(Iterable<Map.Entry<String, Mutable<EditiqueResultatRecu>>> entries) {
						while (!stopping) {
							final Iterator<Map.Entry<String, Mutable<EditiqueResultatRecu>>> iterator = entries.iterator();
							while (iterator.hasNext()) {
								final Map.Entry<String, Mutable<EditiqueResultatRecu>> entry = iterator.next();
								final Pair<Long, RetourImpressionTrigger> trigger = delayedTriggers.remove(entry.getKey());
								if (trigger != null) {
									iterator.remove();
									final Mutable<EditiqueResultatRecu> dh = entry.getValue();
									try {
										if (LOGGER.isDebugEnabled()) {
											final long now = System.nanoTime();
											final String duration = TimeHelper.formatDuree(TimeUnit.NANOSECONDS.toMillis(now - trigger.getLeft()));
											LOGGER.debug(String.format("Exécution du trigger enregistré pour le document '%s' il y a %s", dh.getValue().getIdDocument(), duration));
										}
										trigger.getRight().trigger(dh.getValue());
									}
									catch (Throwable e) {
										LOGGER.error(String.format("Exception levée lors du traitement du document '%s' par le trigger associé", dh.getValue().getIdDocument()), e);
									}
								}
							}

							// on attend le prochain réveil...
							try {
								await();
							}
							catch (InterruptedException e) {
								LOGGER.warn("Le thread des triggers des impressions reçues a été interrompu", e);
								break;
							}
						}
						return null;
					}
				});
			}
			finally {
				LOGGER.info(String.format("Arrêt du thread %s", getName()));
			}
		}

		public void stopIt() {
			doInLockedEnvironment(new Action<String, EditiqueResultatRecu, Object>() {
				@Override
				public Object execute(Iterable<Map.Entry<String, Mutable<EditiqueResultatRecu>>> entries) {
					stopping = true;
					signalAll();
					return null;
				}
			});
		}
	}

	/**
	 * Map des triggers enregistrés pour être déclenchés à la réception de nouveaux retours d'impression
	 * (le Long est le timestamp donné par {@link System#nanoTime()} au moment de l'enregistrement du trigger)
	 */
	private final Map<String, Pair<Long, RetourImpressionTrigger>> delayedTriggers = new HashMap<>();

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
				dateDernierePurgeEffective = new Date();
				super.onPurge(key, value);
			}
		};
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
		if (triggerManagerThread != null) {
			throw new IllegalStateException();
		}
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
	public void registerTrigger(final String nomDocument, final RetourImpressionTrigger trigger) {

		doInLockedEnvironment(new Action<String, EditiqueResultatRecu, Object>() {
			@Override
			public Object execute(Iterable<Map.Entry<String, Mutable<EditiqueResultatRecu>>> entries) {

				// on enregistre le trigger ...
				delayedTriggers.put(nomDocument, Pair.of(System.nanoTime(), trigger));

				// .., et on réveille tout le monde : si le document
				// est en fait déjà là, il sera alors traité par le réveil
				signalAll();
				return null;
			}
		});
	}

	/**
	 * @return une liste des triggers actuellement enregistrés
	 * @see #delayedTriggers
	 */
	public Collection<Pair<Long, RetourImpressionTrigger>> getTriggersEnregistres() {
		return doInLockedEnvironment(new Action<String, EditiqueResultatRecu, Collection<Pair<Long, RetourImpressionTrigger>>>() {
			@Override
			public Collection<Pair<Long, RetourImpressionTrigger>> execute(Iterable<Map.Entry<String, Mutable<EditiqueResultatRecu>>> entries) {
				return new ArrayList<>(delayedTriggers.values());
			}
		});
	}
}
