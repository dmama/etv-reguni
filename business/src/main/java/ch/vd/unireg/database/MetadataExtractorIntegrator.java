package ch.vd.unireg.database;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Intégrateur pour récupérer les méta-data d'Hibernate (voir https://stackoverflow.com/questions/43604928/hibernate-upgrade-to-5-2-session-factory-creation-and-replacing-persistentclas/43718626#43718626)
 */
public class MetadataExtractorIntegrator implements org.hibernate.integrator.spi.Integrator {

	public static final MetadataExtractorIntegrator INSTANCE = new MetadataExtractorIntegrator();

	private Database database;

	public MetadataExtractorIntegrator() {
		int z = 0;
	}

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		database = metadata.getDatabase();
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

	}

	public Database getDatabase() {
		return database;
	}
}
