package ch.vd.unireg.indexer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.DefaultThreadFactory;
import ch.vd.unireg.common.DefaultThreadNameGenerator;

/**
 * Flusher périodique des indexers présents
 */
public class PeriodicIndexFlusher implements InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicIndexFlusher.class);
	private static final Duration ABSOLUTE_MINIMAL_PERIOD = Duration.ofMinutes(1L);

	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> tasks;
	private Map<String, GlobalIndexInterface> indexers;
	private long flushPeriodValue;
	private ChronoUnit flushPeriodUnit;

	public void setIndexers(Map<String, GlobalIndexInterface> indexers) {
		this.indexers = indexers;
	}

	public void setFlushPeriodValue(long flushPeriodValue) {
		this.flushPeriodValue = flushPeriodValue;
	}

	public void setFlushPeriodUnit(ChronoUnit flushPeriodUnit) {
		this.flushPeriodUnit = flushPeriodUnit;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.flushPeriodValue == 0) {
			// désactivation du flush automatique périodique
			LOGGER.info("Flush périodique des indexeurs complètement désactivé.");
		}
		else {
			final Duration period = Duration.of(this.flushPeriodValue, this.flushPeriodUnit);
			if (period.compareTo(ABSOLUTE_MINIMAL_PERIOD) < 0) {
				throw new IllegalArgumentException("La période minimale de flush des indexers est d'une minute (configuration : " + period.toNanos() + " ns)");
			}
			this.scheduler = Executors.newScheduledThreadPool(1, new DefaultThreadFactory(new DefaultThreadNameGenerator("IndexFlusher")));

			final long millis = period.toMillis();
			this.tasks = this.scheduler.scheduleWithFixedDelay(this::flush, millis, millis, TimeUnit.MILLISECONDS);

			LOGGER.info("Flush des indexeurs avec une période de " + millis + " millisecondes");
		}
	}

	@Override
	public void destroy() throws Exception {
		if (this.scheduler != null) {
			try {
				if (this.tasks != null) {
					this.tasks.cancel(false);
				}
				this.scheduler.shutdown();
				while (!this.scheduler.isTerminated()) {
					this.scheduler.awaitTermination(10L, TimeUnit.SECONDS);
				}
			}
			finally {
				this.scheduler.shutdownNow();
			}
		}
	}

	private void flush() {
		if (indexers != null) {
			indexers.forEach(this::flushIndexer);
		}
	}

	private void flushIndexer(String name, GlobalIndexInterface index) {
		try {
			LOGGER.debug(String.format("Flush de l'indexeur %s", name));
			index.flush();
		}
		catch (Exception e) {
			LOGGER.error(String.format("Exception levée pendant le flush de l'indexeur %s", name), e);
		}
	}
}
