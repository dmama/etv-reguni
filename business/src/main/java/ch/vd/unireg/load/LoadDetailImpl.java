package ch.vd.unireg.load;

import java.time.Duration;
import java.time.Instant;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.stats.LoadDetail;

public class LoadDetailImpl<T> implements LoadDetail {
	
	private final T descriptor;
	private final Instant start;
	private final String threadName;
	private final StringRenderer<? super T> renderer;
	
	public LoadDetailImpl(T descriptor, Instant start, String threadName, StringRenderer<? super T> renderer) {
		this.descriptor = descriptor;
		this.start = start;
		this.threadName = threadName;
		this.renderer = renderer;
	}
	
	@Override
	public String getDescription() {
		return renderer.toString(descriptor);
	}

	@Override
	public Duration getDuration() {
		return Duration.between(start, InstantHelper.get());
	}

	@Override
	public String getThreadName() {
		return threadName;
	}
}
