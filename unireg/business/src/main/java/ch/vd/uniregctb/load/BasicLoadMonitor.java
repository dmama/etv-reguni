package ch.vd.uniregctb.load;

import ch.vd.uniregctb.stats.LoadMonitor;
import ch.vd.uniregctb.stats.LoadMonitorable;

public class BasicLoadMonitor implements LoadMonitor {

	private final LoadMonitorable service;
	private final LoadAverager averager;

	public BasicLoadMonitor(LoadMonitorable service, LoadAverager fiveMinuteAverager) {
		this.service = service;
		this.averager = fiveMinuteAverager;
	}

	@Override
	public int getLoad() {
		return service.getLoad();
	}

	@Override
	public double getFiveMinuteAverageLoad() {
		return averager.getAverageLoad();
	}

	@Override
	public LoadMonitorable getMonitorable() {
		return service;
	}
}
