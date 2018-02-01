package ch.vd.unireg.efacture;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.AsyncStorage;
import ch.vd.unireg.common.AsyncStorageWithPeriodicCleanup;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class EFactureResponseServiceImpl implements EFactureResponseService, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "ReponseEFacture";

	private static final Logger LOGGER = LoggerFactory.getLogger(EFactureResponseServiceImpl.class);

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
					super.onPurge(key, value);
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
			serviceTracing.end(start, "onNewResponse", () -> String.format("businessId='%s'", businessId));
		}
	}

	@Override
	public boolean waitForResponse(final String businessId, final Duration timeout) {
		if (timeout.isNegative() || timeout.isZero()) {
			throw new IllegalArgumentException(String.format("timeout devrait être strictement positif (%s)", timeout));
		}

		final long start = serviceTracing.start();
		try {
			final AsyncStorage.RetrievalResult<String> result = storage.get(businessId, timeout);
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
			serviceTracing.end(start, "waitForResponse", () -> String.format("businessId='%s', timeout=%dms", businessId, timeout.toMillis()));
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
