package ch.vd.uniregctb.indexer;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GlobalIndexTracing implements GlobalIndexInterface, ServiceTracingInterface, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "GlobalIndex";

	private GlobalIndexInterface target;
	private final ServiceTracing tracing = new ServiceTracing();

	public void setTarget(GlobalIndexInterface target) {
		this.target = target;
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

	public void search(Query query, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, callback);
		}
		finally {
			tracing.end(time);
		}
	}

	public void search(String query, SearchCallback callback) throws IndexerException {
		long time = tracing.start();
		try {
			target.search(query, callback);
		}
		finally {
			tracing.end(time);
		}
	}

	public long getLastCallTime() {
		return tracing.getLastCallTime();
	}

	public long getTotalTime() {
		return tracing.getTotalTime();
	}

	public long getTotalPing() {
		return tracing.getTotalPing();
	}

	public long getRecentTime() {
		return tracing.getRecentTime();
	}

	public long getRecentPing() {
		return tracing.getRecentPing();
	}

	public void afterPropertiesSet() throws Exception {
		StatsService.registerRawService(SERVICE_NAME, this);
	}

	public void destroy() throws Exception {
		StatsService.unregisterRawService(SERVICE_NAME);
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}
}
