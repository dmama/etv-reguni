package ch.vd.uniregctb.migration.pm;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class Migrator implements SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);

	private MigrationMode mode;
	private FromDbFeeder fromDbFeeder;
	private SerializationIntermediary serializationIntermediary;
	private MigrationWorker migrationWorker;

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

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		thread = new Thread("Migrator") {
			@Override
			public void run() {
				migrate();
			}
		};
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
		LOGGER.info("Démarrage de la migration...");
		try {

			// le mode est obligatoire !
			if (mode == null) {
				throw new IllegalStateException("Le mode est obligatoire ! : " + Arrays.toString(MigrationMode.values()));
			}

			final Feeder feeder = mode == MigrationMode.FROM_DUMP ? serializationIntermediary : fromDbFeeder;
			final Worker worker = mode == MigrationMode.DUMP ? serializationIntermediary : migrationWorker;

			try {
				feeder.feed(worker);
			}
			finally {
				worker.feedingOver();
			}
		}
		catch (Exception e) {
			LOGGER.error("Exception levée dans le thread principal de migration", e);
		}
		finally {
			LOGGER.info("Fin de la migration.");
		}
	}
}
