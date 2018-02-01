package ch.vd.uniregctb.indexer;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GlobalIndexTracing implements GlobalIndexInterface, InitializingBean, DisposableBean {

	private GlobalIndexInterface target;
	private StatsService statsService;
	private String serviceName;

	private ServiceTracing tracing;

	public void setTarget(GlobalIndexInterface target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@Override
	public void flush() throws IndexerException {
		long time = tracing.start();
		try {
			target.flush();
		}
		finally {
			tracing.end(time, "flush", null);
		}
	}

	@Override
	public int getApproxDocCount() {
		int result;
		long time = tracing.start();
		try {
			result = target.getApproxDocCount();
		}
		finally {
			tracing.end(time, "getApproxDocCount", null);
		}
		return result;
	}

	@Override
	public int getExactDocCount() {
		int result;
		long time = tracing.start();
		try {
			result = target.getExactDocCount();
		}
		finally {
			tracing.end(time, "getExactDocCount", null);
		}
		return result;
	}

	@Override
	public String getIndexPath() {
		String result;
		long time = tracing.start();
		try {
			result = target.getIndexPath();
		}
		finally {
			tracing.end(time, "getIndexPath", null);
		}
		return result;
	}

	@Override
	public void indexEntity(final IndexableData data) {
		long time = tracing.start();
		try {
			target.indexEntity(data);
		}
		finally {
			tracing.end(time, "indexEntity", () -> String.format("id=%d, type=%s, subtype=%s", data.getId(), data.getType(), data.getSubType()));
		}
	}

	@Override
	public void indexEntities(final List<IndexableData> data) {
		long time = tracing.start();
		try {
			target.indexEntities(data);
		}
		finally {
			tracing.end(time, "indexEntities", () -> String.format("data=%s", ServiceTracing.toString(data)));
		}
	}

	@Override
	public void optimize() throws IndexerException {
		long time = tracing.start();
		try {
			target.optimize();
		}
		finally {
			tracing.end(time, "optimize", null);
		}
	}

	@Override
	public void overwriteIndex() {
		long time = tracing.start();
		try {
			target.overwriteIndex();
		}
		finally {
			tracing.end(time, "overwriteIndex", null);
		}
	}

	@Override
	public void removeEntity(final Long id) throws IndexerException {
		long time = tracing.start();
		try {
			target.removeEntity(id);
		}
		finally {
			tracing.end(time, "removeEntity", () -> String.format("id=%d", id));
		}
	}

	@Override
	public void deleteEntitiesMatching(@NotNull Query query) {
		long time = tracing.start();
		try {
			target.deleteEntitiesMatching(query);
		}
		finally {
			tracing.end(time, "deleteEntitiesMatching", () -> "query=" + query);
		}
	}

	@Override
	public void removeThenIndexEntity(final IndexableData data) {
		long time = tracing.start();
		try {
			target.removeThenIndexEntity(data);
		}
		finally {
			tracing.end(time, "removeThenIndexEntity", () -> String.format("id=%d, type=%s, subtype=%s", data.getId(), data.getType(), data.getSubType()));
		}
	}

	@Override
	public void removeThenIndexEntities(final List<IndexableData> data) {
		long time = tracing.start();
		try {
			target.removeThenIndexEntities(data);
		}
		finally {
			tracing.end(time, "removeThenIndexEntities", () -> String.format("data=%s", ServiceTracing.toString(data)));
		}
	}

	@Override
	public int deleteDuplicate() {
		long time = tracing.start();
		try {
			return target.deleteDuplicate();
		}
		finally {
			tracing.end(time, "deleteDuplicate", null);
		}
	}

	@Override
	public void search(final Query query, final int maxHits, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, maxHits, callback);
		}
		finally {
			tracing.end(time, "search", () -> String.format("query='%s', maxHits=%d", query, maxHits));
		}
	}

	@Override
	public void search(final String query, final int maxHits, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, maxHits, callback);
		}
		finally {
			tracing.end(time, "search", () -> String.format("queryString='%s', maxHits=%d", query, maxHits));
		}
	}

	@Override
	public void search(final Query query, final int maxHits, final Sort sort, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, maxHits, sort, callback);
		}
		finally {
			tracing.end(time, "search", () -> String.format("query='%s', maxHits=%d, sorting=%s", query, maxHits, sort));
		}
	}

	@Override
	public void searchAll(Query query, SearchAllCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.searchAll(query, callback);
		}
		finally {
			tracing.end(time, "searchAll", null);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		tracing = new ServiceTracing(serviceName);
		if (statsService != null) {
			statsService.registerService(serviceName, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(serviceName);
		}
	}
}
