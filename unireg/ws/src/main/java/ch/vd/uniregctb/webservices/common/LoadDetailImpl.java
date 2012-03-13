package ch.vd.uniregctb.webservices.common;

import java.util.concurrent.TimeUnit;

public class LoadDetailImpl implements LoadDetail {
	
	private final Object descriptor;
	private final long nanoStart;
	
	public LoadDetailImpl(Object descriptor, long nanoStart) {
		this.descriptor = descriptor;
		this.nanoStart = nanoStart;
	}
	
	@Override
	public String getDescription() {
		return descriptor != null ? descriptor.toString() : null;
	}

	@Override
	public long getDurationMs() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoStart);
	}
}
