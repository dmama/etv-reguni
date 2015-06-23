package ch.vd.uniregctb.migration.pm.engine;

import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;

/**
 * Interface principal des migrateurs d'entité
 * @param <T> le type d'entité migrée
 */
public interface EntityMigrator<T extends RegpmEntity> {

	/**
	 * Permet d'initialiser des structures de données dans le résultat avant toute migration
	 * @param mr structure à initialiser
	 */
	void initMigrationResult(MigrationResultInitialization mr);

	/**
	 * Migre l'entité donnée
	 * @param entity entité à migrer
	 * @param mr récipiendaire de messages à logguer
	 * @param linkCollector collecteur de liens entre entités (seront résolus à la fin de la migration)
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	void migrate(T entity, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper);
}
