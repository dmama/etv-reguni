package ch.vd.uniregctb.editique.impl;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.AsyncStorage;
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
	 * Espace de stockage des documents reçus et pas encore réclamés
	 */
	private EditiqueRetourImpressionStorage impressionsRecues;

	/**
	 * Période du timer du cleanup (secondes) : à chaque tick, on va enlever de la map des impressions
	 * reçues les données qui étaient déjà là au tick précédent
	 */
	private int cleanupPeriod;

	public void setCleanupPeriod(int cleanupPeriod) {
		if (cleanupPeriod <= 0) {
			throw new IllegalArgumentException("La valeur doit être strictement positive");
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Le délai de purge des documents imprimés non réclamés est de %d seconde%s.", cleanupPeriod, cleanupPeriod > 1 ? "s" : ""));
		}
		this.cleanupPeriod = cleanupPeriod;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public int getCleanupPeriod() {
		return cleanupPeriod;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (cleanupPeriod <= 0) {
			throw new IllegalArgumentException("La valeur de 'cleanupPeriod' doit être strictement positive");
		}
		impressionsRecues = new EditiqueRetourImpressionStorage(cleanupPeriod);
		impressionsRecues.start();
		statsService.registerService(SERVICE_NAME, serviceTracing);
	}

	@Override
	public void destroy() throws Exception {
		impressionsRecues.stop();
		statsService.unregisterService(SERVICE_NAME);
	}

	/**
	 * Enregistre un trigger qui sera déclenché à la réception du retour d'impression identifié par son ID
	 * @param nomDocument ID du document déclencheur
	 * @param trigger action à lancer à la réception du document voulu
	 */
	@Override
	public void registerTrigger(String nomDocument, RetourImpressionTrigger trigger) {
		impressionsRecues.registerTrigger(nomDocument, trigger);
	}

	@Override
	public void onArriveeRetourImpression(EditiqueResultatRecu resultat) {

		// ah ? un retour d'impression ? il faut le mettre dans l'espace de stockage

		final long start = serviceTracing.start();
		final String idDocument = resultat.getIdDocument();
		try {
			impressionsRecues.add(idDocument, resultat);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Réception du document imprimé '%s'", idDocument));
			}
		}
		finally {
			serviceTracing.end(start, "onArriveeRetourImpression", new Object() {
				@Override
				public String toString() {
					return String.format("idDocument='%s'", idDocument);
				}
			});
		}
	}

	@Override
	public EditiqueResultat getDocument(final String nomDocument, final long timeout) {

		Assert.isTrue(timeout > 0);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Demande de récupération du document '%s'", nomDocument));
		}

		final long start = serviceTracing.start();
		try {
			final AsyncStorage.RetrievalResult<String> res = impressionsRecues.get(nomDocument, timeout, TimeUnit.MILLISECONDS);
			if (res instanceof AsyncStorage.RetrievalTimeout) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Timeout dépassé pour la récupération du document '%s'", nomDocument));
				}
				return new EditiqueResultatTimeoutImpl(nomDocument);
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Document '%s' trouvé", nomDocument));
			}
			return ((AsyncStorage.RetrievalData<String, EditiqueResultatRecu>) res).data;
		}
		catch (InterruptedException e) {
			// interrompu...? on s'en va
			return new EditiqueResultatTimeoutImpl(nomDocument);
		}
		finally {
			serviceTracing.end(start, "getDocument", new Object() {
				@Override
				public String toString() {
					return String.format("idDocument='%s', timeout=%dms", nomDocument, timeout);
				}
			});
		}
	}

	@Override
	public int getDocumentsEnAttenteDeDispatch() {
		return impressionsRecues.size();
	}

	@Override
	public int getDocumentsPurges() {
		return impressionsRecues.getNbPurgedElements();
	}

	@Override
	public Date getDateDernierePurgeEffective() {
		return impressionsRecues.getDateDernierePurgeEffective();
	}

	@Override
	public int getDocumentsRecus() {
		return impressionsRecues.getNbReceived();
	}

	@Override
	public Collection<Pair<Long, RetourImpressionTrigger>> getTriggersEnregistres() {
		return impressionsRecues.getTriggersEnregistres();
	}
}
