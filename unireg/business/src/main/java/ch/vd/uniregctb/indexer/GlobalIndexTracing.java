package ch.vd.uniregctb.indexer;

import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.stats.StatsService;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GlobalIndexTracing implements GlobalIndexInterface, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "GlobalIndex";

	private GlobalIndexInterface target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing();

	public void setTarget(GlobalIndexInterface target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void flush() throws IndexerException {
		long time = tracing.start();
		try {
			target.flush();
		}
		finally {
			tracing.end(time);
		}
	}

	public int getApproxDocCount() {
		int result;
		long time = tracing.start();
		try {
			result = target.getApproxDocCount();
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public int getExactDocCount() {
		int result;
		long time = tracing.start();
		try {
			result = target.getExactDocCount();
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public String getIndexPath() throws Exception {
		String result;
		long time = tracing.start();
		try {
			result = target.getIndexPath();
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public void indexEntity(IndexableData data) {
		long time = tracing.start();
		try {
			target.indexEntity(data);
		}
		finally {
			tracing.end(time);
		}
	}

	public void indexEntities(List<IndexableData> data) {
		long time = tracing.start();
		try {
			target.indexEntities(data);
		}
		finally {
			tracing.end(time);
		}
	}

	public void optimize() throws IndexerException {
		long time = tracing.start();
		try {
			target.optimize();
		}
		finally {
			tracing.end(time);
		}
	}

	public void overwriteIndex() {
		long time = tracing.start();
		try {
			target.overwriteIndex();
		}
		finally {
			tracing.end(time);
		}
	}

	public void removeEntity(Long id, String type) throws IndexerException {
		long time = tracing.start();
		try {
			target.removeEntity(id, type);
		}
		finally {
			tracing.end(time);
		}
	}

	public void removeThenIndexEntity(IndexableData data) {
		long time = tracing.start();
		try {
			target.removeThenIndexEntity(data);
		}
		finally {
			tracing.end(time);
		}
	}

	public void removeThenIndexEntities(List<IndexableData> data) {
		long time = tracing.start();
		try {
			target.removeThenIndexEntities(data);
		}
		finally {
			tracing.end(time);
		}
	}

	public void search(Query query, int maxHits, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, maxHits, callback);
		}
		finally {
			tracing.end(time);
		}
	}

	public void search(String query, int maxHits, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, maxHits, callback);
		}
		finally {
			tracing.end(time);
		}
	}

	public void searchAll(Query query, SearchAllCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.searchAll(query, callback);
		}
		finally {
			tracing.end(time);
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}
}
