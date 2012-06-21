package ch.vd.uniregctb.efacture;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.AsyncStorage;
import ch.vd.uniregctb.common.AsyncStorageWithPeriodicCleanup;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class EFactureResponseServiceImpl implements EFactureResponseService, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "ReponseEFacture";

	private static final Logger LOGGER = Logger.getLogger(EFactureResponseServiceImpl.class);

	private ResponseStorage storage;

	private final ServiceTracing serviceTracing = new ServiceTracing(SERVICE_NAME);

	private StatsService statsService;

	/**
	 * En secondes, la période de cleanup
	 */
	private int cleanupPeriod;

	/**
	 * Implémentation locale de l'espace de stockage dans lequel les réponses sont emmagasinées
	 */
	private static class ResponseStorage extends AsyncStorageWithPeriodicCleanup<String, Object> {

		private ResponseStorage(int cleanupPeriodSeconds) {
			super(cleanupPeriodSeconds, "ResponseEFactureCleanup");
		}

		@Override
		protected CleanupTask buildCleanupTask() {
			return new CleanupTask() {
				@Override
				protected void onPurge(String key, Object value) {
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info(String.format("Purge de la réponse e-facture au message '%s' qui n'intéresse apparemment personne", key));
					}
				}
			};
		}
	}

	public void setCleanupPeriod(int cleanupPeriod) {
		this.cleanupPeriod = cleanupPeriod;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void onNewResponse(String businessId) {
		final long start = serviceTracing.start();
		try {
			storage.add(businessId, null);
		}
		finally {
			serviceTracing.end(start, "onNewResponse", businessId);
		}
	}

	@Override
	public boolean waitForResponse(String businessId, long timeoutMs) {
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException(String.format("timeout devrait être strictement positif (%d)", timeoutMs));
		}

		final long start = serviceTracing.start();
		try {
			final AsyncStorage.RetrievalResult<String> result = storage.get(businessId, timeoutMs, TimeUnit.MILLISECONDS);
			if (result instanceof AsyncStorage.RetrievalTimeout) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info(String.format("Timeout atteint pendant l'attente de la réponse e-facture au message '%s'", businessId));
				}
			}
			return (result instanceof AsyncStorage.RetrievalData);
		}
		catch (InterruptedException e) {
			// et bien tant pis !
			return false;
		}
		finally {
			serviceTracing.end(start, "waitForResponse", businessId);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		statsService.registerService(SERVICE_NAME, serviceTracing);
		if (cleanupPeriod <= 0) {
			throw new IllegalArgumentException("La valeur de cleanupPeriod doit être strictement positive.");
		}
		storage = new ResponseStorage(cleanupPeriod);
		storage.start();
	}

	@Override
	public void destroy() throws Exception {
		if (storage != null) {
			storage.stop();
			storage = null;
		}
		statsService.unregisterService(SERVICE_NAME);
	}
}
