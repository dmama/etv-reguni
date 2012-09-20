package ch.vd.uniregctb.load;

import java.util.concurrent.TimeUnit;

public class LoadDetailImpl<T> implements LoadDetail {
	
	private final T descriptor;
	private final long nanoStart;
	private final LoadDetailRenderer<T> renderer;
	
	public LoadDetailImpl(T descriptor, long nanoStart, LoadDetailRenderer<T> renderer) {
		this.descriptor = descriptor;
		this.nanoStart = nanoStart;
		this.renderer = renderer;
	}
	
	@Override
	public String getDescription() {
		return renderer.toString(descriptor);
	}

	@Override
	public long getDurationMs() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoStart);
	}
}
