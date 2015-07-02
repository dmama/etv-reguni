package ch.vd.uniregctb.migration.pm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import ch.vd.uniregctb.migration.pm.engine.MigrationWorker;

public class Migrator implements SmartLifecycle, MigrationInitializationRegistrar {

	private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);

	private MigrationMode mode;
	private FromDbFeeder fromDbFeeder;
	private SerializationIntermediary serializationIntermediary;
	private MigrationWorker migrationWorker;
	private boolean enabled = true;

	private final List<Runnable> initCallbacks = new LinkedList<>();

	private Thread thread;

	public void setMode(MigrationMode mode) {
		this.mode = mode;
	}

	public void setFromDbFeeder(FromDbFeeder fromDbFeeder) {
		this.fromDbFeeder = fromDbFeeder;
	}

	public void setSerializationIntermediary(SerializationIntermediary serializationIntermediary) {
		this.serializationIntermediary = serializationIntermediary;
	}

	public void setMigrationWorker(MigrationWorker migrationWorker) {
		this.migrationWorker = migrationWorker;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isAutoStartup() {
		return enabled;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		thread = new Thread(this::migrate, "Migrator");
		thread.start();
	}

	@Override
	public void stop() {
		thread.interrupt();
		try {
			thread.join();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return thread != null && thread.isAlive();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	private void migrate() {
		LOGGER.info(String.format("Démarrage de la migration (mode %s)...", mode));
		try {

			// le mode est obligatoire !
			if (mode == null) {
				throw new IllegalStateException("Le mode est obligatoire ! : " + Arrays.toString(MigrationMode.values()));
			}

			// phase d'initialization ?
			synchronized (initCallbacks) {
				if (!initCallbacks.isEmpty()) {
					try {
						LOGGER.info("Lancement de la procédure d'initialisation.");
						initCallbacks.forEach(Runnable::run);
						LOGGER.info("Procédure d'initialisation terminée.");
					}
					catch (Exception e) {
						// on va sortir, il faut dire au worker de s'arrêter...
						migrationWorker.feedingOver();
						throw e;
					}
				}
			}

			if (mode != MigrationMode.NOOP) {
				final Feeder feeder = mode == MigrationMode.FROM_DUMP ? serializationIntermediary : fromDbFeeder;
				final Worker worker = mode == MigrationMode.DUMP ? serializationIntermediary : migrationWorker;

				try {
					feeder.feed(worker);
				}
				finally {
					worker.feedingOver();
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Exception levée dans le thread principal de migration", e);
		}
		finally {
			LOGGER.info("Fin de la migration.");
		}
	}

	@Override
	public void registerInitializationCallback(@NotNull Runnable callback) {
		if (thread != null) {
			throw new IllegalStateException("Le processus a déjà commencé : il est trop tard pour enregistrer un callbck d'initialisation !");
		}

		synchronized (initCallbacks) {
			initCallbacks.add(callback);
		}
	}
}
