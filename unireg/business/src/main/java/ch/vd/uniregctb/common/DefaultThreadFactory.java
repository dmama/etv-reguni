package ch.vd.uniregctb.common;

import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Classe de {@link ThreadFactory} qui permet de donner un nom à chaque thread en utilisant un {@link ThreadNameGenerator}
 */
public final class DefaultThreadFactory implements ThreadFactory {

	private static final Logger LOGGER = Logger.getLogger(DefaultThreadFactory.class);

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
		return new Thread(r, threadNameGenerator.getNewThreadName()) {
			@Override
			public void run() {
				LOGGER.info(String.format("Démarrage du thread %s", getName()));
				try {
					super.run();
				}
				catch (RuntimeException | Error e) {
					LOGGER.error(String.format("Explosion du thread %s", getName()), e);
					throw e;
				}
				finally {
					LOGGER.info(String.format("Arrêt du thread %s", getName()));
				}
			}
		};
	}
}
