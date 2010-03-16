package ch.vd.uniregctb.performance;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sebastien.diaz@vd.ch">S�bastien Diaz </a> * @author <a href="mailto:sebastien.diaz@vd.ch">S�bastien Diaz</a>
 */
public class PerformanceLogsRepository {

	//private final static Logger LOGGER = Logger.getLogger(PerformanceLogsRepository.class);

	public static final String CONTROLLER = "controller";
	public static final String DAO = "dao";
	public static final String SERVICE = "service";

	private static PerformanceLogsRepository instance;

	private final Map<String, Map<String, PerformanceLog>> layers = new HashMap<String, Map<String, PerformanceLog>>();

	private PerformanceLogsRepository() {
	}

	public static final PerformanceLogsRepository getInstance() {
		if (instance == null) {
			instance = new PerformanceLogsRepository();
		}
		return instance;
	}

	/**
	 * @return Returns the logs.
	 */
	public Map<String, Map<String, PerformanceLog>> getLayers() {
		return layers;
	}

	/**
	 * @param layer
	 * @return
	 */
	public Map<String, PerformanceLog> getLogs(String layer) {
		Map<String, PerformanceLog> logs = layers.get(layer);
		if (logs == null) {
			logs = new HashMap<String, PerformanceLog>();
		}
		return logs;
	}

	/**
	 * @param layer
	 * @param item
	 * @param duration
	 */
	public void addLog(String layer, String item, long duration) {
		Map<String, PerformanceLog> logs = getLogs(layer);
		PerformanceLog performanceLog = logs.get(item);
		if (performanceLog == null) {
			performanceLog = new PerformanceLog(item);
		}
		performanceLog.record(duration);
		logs.put(item, performanceLog);
		layers.put(layer, logs);
	}
}
