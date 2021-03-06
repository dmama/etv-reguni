package ch.vd.unireg.indexer.tiers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.Fuse;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersFilter;

public class GlobalTiersSearcherTracing implements GlobalTiersSearcher, InitializingBean, DisposableBean {

	private GlobalTiersSearcher target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(GlobalTiersSearcher target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public List<TiersIndexedData> search(final TiersCriteria criteria) throws IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.search(criteria);
		}
		catch (IndexerException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "search", () -> String.format("criteria={%s}", criteria));
		}
	}

	@Override
	public TopList<TiersIndexedData> searchTop(final TiersCriteria criteria, final int max) throws IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.searchTop(criteria, max);
		}
		catch (IndexerException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "searchTop", () -> String.format("criteria={%s}, max=%d", criteria, max));
		}
	}

	@Override
	public TopList<TiersIndexedData> searchTop(final String keywords, final TiersFilter filter, final int max) throws IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.searchTop(keywords, filter, max);
		}
		catch (IndexerException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "searchTop", () -> String.format("keywords='%s', max=%d", keywords, max));
		}
	}

	@Override
	public boolean exists(final Long numero) throws IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.exists(numero);
		}
		catch (IndexerException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "exists", () -> String.format("numero=%s", numero));
		}
	}

	@Override
	public TiersIndexedData get(final Long numero) throws IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.get(numero);
		}
		catch (IndexerException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "get", () -> String.format("numero=%s", numero));
		}
	}

	@Override
	public Set<Long> getAllIds() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAllIds();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAllIds", null);
		}
	}

	@Override
	public void checkCoherenceIndex(Set<Long> existingIds, StatusManager statusManager, CheckCallback callback) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.checkCoherenceIndex(existingIds, statusManager, callback);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "checkCoherenceIndex", null);
		}
	}

	@Override
	public int getApproxDocCount() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getApproxDocCount();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getApproxDocCount", null);
		}
	}

	@Override
	public int getExactDocCount() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getExactDocCount();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getExactDocCount", null);
		}
	}

	@Override
	public void flowSearch(final TiersCriteria criteria, BlockingQueue<TiersIndexedData> queue, Fuse fusible) throws IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.flowSearch(criteria, queue, fusible);
		}
		catch (IndexerException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "flowSearch", () -> String.format("criteria={%s}", criteria));
		}
	}
}
