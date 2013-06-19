package ch.vd.uniregctb.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class DefaultThreadNameGenerator implements ThreadNameGenerator {

	private final AtomicInteger sequence = new AtomicInteger(0);
	private final String prefixe;

	public DefaultThreadNameGenerator(String prefixe) {
		this.prefixe = prefixe;
	}

	@Override
	public String getNewThreadName() {
		return String.format("%s-%d", prefixe, sequence.getAndIncrement());
	}
}
