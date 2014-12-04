package ch.vd.uniregctb.common;

import java.util.concurrent.ThreadFactory;

import org.jetbrains.annotations.NotNull;

/**
 * Classe de {@link ThreadFactory} qui permet de donner un nom à chaque thread en utilisant un {@link ThreadNameGenerator}
 */
public final class DefaultThreadFactory implements ThreadFactory {

	private final ThreadNameGenerator threadNameGenerator;

	public DefaultThreadFactory(ThreadNameGenerator threadNameGenerator) {
		if (threadNameGenerator == null) {
			throw new NullPointerException("threadNameGenerator");
		}
		this.threadNameGenerator = threadNameGenerator;
	}

	@NotNull
	@Override
	public Thread newThread(@NotNull Runnable r) {
		return new Thread(r, threadNameGenerator.getNewThreadName());
	}
}
