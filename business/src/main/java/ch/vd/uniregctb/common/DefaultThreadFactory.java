package ch.vd.uniregctb.common;

import java.util.concurrent.ThreadFactory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de {@link ThreadFactory} qui permet de donner un nom à chaque thread en utilisant un {@link ThreadNameGenerator}
 */
public final class DefaultThreadFactory implements ThreadFactory {

	private final ThreadNameGenerator threadNameGenerator;
	private final Boolean daemon;

	public DefaultThreadFactory(ThreadNameGenerator threadNameGenerator) {
		this(threadNameGenerator, null);
	}

	public DefaultThreadFactory(ThreadNameGenerator threadNameGenerator, @Nullable Boolean daemon) {
		if (threadNameGenerator == null) {
			throw new NullPointerException("threadNameGenerator");
		}
		this.threadNameGenerator = threadNameGenerator;
		this.daemon = daemon;
	}

	@NotNull
	@Override
	public Thread newThread(@NotNull Runnable r) {
		final Thread thread = new Thread(r, threadNameGenerator.getNewThreadName());
		if (daemon != null) {
			thread.setDaemon(daemon);
		}
		return thread;
	}
}
