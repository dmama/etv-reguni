package ch.vd.uniregctb.load;

import java.util.concurrent.TimeUnit;

import ch.vd.uniregctb.common.StringRenderer;

public class LoadDetailImpl<T> implements LoadDetail {
	
	private final T descriptor;
	private final long nanoStart;
	private final String threadName;
	private final StringRenderer<? super T> renderer;
	
	public LoadDetailImpl(T descriptor, long nanoStart, String threadName, StringRenderer<? super T> renderer) {
		this.descriptor = descriptor;
		this.nanoStart = nanoStart;
		this.threadName = threadName;
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

	@Override
	public String getThreadName() {
		return threadName;
	}
}
