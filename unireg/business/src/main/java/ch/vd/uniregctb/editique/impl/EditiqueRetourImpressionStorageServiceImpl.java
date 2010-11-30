package ch.vd.uniregctb.editique.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;

/**
 * Implémentation du service de stockage des retours d'impression de l'éditique
 */
public class EditiqueRetourImpressionStorageServiceImpl implements EditiqueRetourImpressionStorageService, InitializingBean, DisposableBean {

	public final static Logger LOGGER = Logger.getLogger(EditiqueRetourImpressionStorageServiceImpl.class);

	private final Map<String, EditiqueResultat> impressionsRecues = new HashMap<String, EditiqueResultat>();

	private Timer cleanupTimer = null;

	private TimerTask cleanupTask = null;

	private Date dateDernierePurgeEffective = null;

	private int purgedDocuments = 0;

	private int receivedDocuments = 0;

	/**
	 * Période du timer du cleanup (secondes) : à chaque tick, on va enlever de la map des impressions
	 * reçues les données qui étaient déjà là au tick précédent
	 */
	private int cleanupPeriod;

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

	private void restartCleanupTimer() {
		if (cleanupTimer != null) {
			if (cleanupTask != null) {
				cleanupTask.cancel();
			}
			cleanupTask = new CleanupTask();
			cleanupTimer.schedule(cleanupTask, cleanupPeriod * 1000L, cleanupPeriod * 1000L);
		}
	}

	public int getCleanupPeriod() {
		return cleanupPeriod;
	}

	/**
	 * Tâche de cleanup des vieux retours d'impressions que personne n'a demandé
	 */
	private final class CleanupTask extends TimerTask {

		@Override
		public void run() {
			final long tickPrecedent = System.currentTimeMillis() - cleanupPeriod * 1000L;
			synchronized (impressionsRecues) {
				final Iterator<Map.Entry<String, EditiqueResultat>> iterator = impressionsRecues.entrySet().iterator();
				while (iterator.hasNext()) {
					final Map.Entry<String, EditiqueResultat> entry = iterator.next();
					final EditiqueResultat document = entry.getValue();
					if (document.getTimestampReceived() < tickPrecedent) {
						LOGGER.warn(String.format("Cleanup du retour d'impression '%s' qui n'intéresse apparemment personne", document.getIdDocument()));
						iterator.remove();

						++ purgedDocuments;
						dateDernierePurgeEffective = new Date();
					}
				}
			}
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (cleanupPeriod <= 0) {
			throw new IllegalArgumentException("La valeur de 'cleanupPeriod' doit être strictement positive");
		}
		cleanupTimer = new Timer("RetourImpressionCleanup");
		restartCleanupTimer();
	}

	public void destroy() throws Exception {
		cleanupTimer.cancel();
		cleanupTimer = null;
	}

	public void onArriveeRetourImpression(EditiqueResultat resultat) {

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

	/**
	 * @return un timestamp correspondant à l'instant actuel en millisecondes
	 */
	private static long getTimestampMillis() {
		return System.nanoTime() / 1000000L;
	}

	public EditiqueResultat getDocument(String nomDocument, long timeout) {

		Assert.isTrue(timeout > 0);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Demande de récupération du document '%s'", nomDocument));
		}

		final long tsAttente = getTimestampMillis() + timeout;        // on n'attendra pas plus tard...

		synchronized (impressionsRecues) {

			// on attends le temps qu'il faut...
			while (true) {

				// déjà là ?
				final EditiqueResultat resultat = impressionsRecues.remove(nomDocument);
				if (resultat == null) {

					// et non, on attends un peu... mais pas trop quand-même !
					final long tempsRestant = tsAttente - getTimestampMillis();
					if (tempsRestant <= 0) {

						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug(String.format("Timeout dépassé pour la récupération du document '%s'", nomDocument));
						}

						return null;
					}

					try {
						impressionsRecues.wait(tempsRestant);
					}
					catch (InterruptedException e) {
						// interrompu...? on s'en va
						return null;
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

	public int getDocumentsEnAttenteDeDispatch() {
		synchronized (impressionsRecues) {
			return impressionsRecues.size();
		}
	}

	public int getDocumentsPurges() {
		return purgedDocuments;
	}

	public Date getDateDernierePurgeEffective() {
		return dateDernierePurgeEffective;
	}

	public int getDocumentsRecus() {
		return receivedDocuments;
	}
}
