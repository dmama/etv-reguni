package ch.vd.uniregctb.migration.pm.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.migration.pm.engine.MigrationWorker;

@ManagedResource
public class EngineJmxBean {

	private MigrationWorker worker;

	public void setWorker(MigrationWorker worker) {
		this.worker = worker;
	}

	@ManagedAttribute
	public int getFeedingQueueSize() {
		return worker.getTailleFileAttente();
	}

	@ManagedAttribute
	public int getBusyThreads() {
		return worker.getNombreMigrationsEnCours();
	}
}
