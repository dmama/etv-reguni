package ch.vd.uniregctb.migration.pm;

import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;

public abstract class AbstractEntityMigrator<T extends RegpmEntity> implements EntityMigrator<T> {

	protected final SessionFactory uniregSessionFactory;
	protected final StreetDataMigrator streetDataMigrator;
	protected final TiersDAO tiersDAO;

	protected AbstractEntityMigrator(SessionFactory uniregSessionFactory, StreetDataMigrator streetDataMigrator, TiersDAO tiersDAO) {
		this.uniregSessionFactory = uniregSessionFactory;
		this.streetDataMigrator = streetDataMigrator;
		this.tiersDAO = tiersDAO;
	}

	/**
	 * @param entity entité à migrer
	 * @param mr récipiendaire de messages à logguer
	 * @param linkCollector collecteur de liens entre entités (seront résolus à la fin de la migration)
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	@Override
	public final void migrate(T entity, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		final MigrationResult localMr = new MigrationResult();
		try {
			doMigrate(entity, localMr, linkCollector, idMapper);
		}
		finally {
			mr.addAll(localMr, getMessagePrefix(entity));
		}
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByUniregIdSupplier(long id) {
		return () -> getEntityFromDb(Entreprise.class, id);
	}

	@NotNull
	protected final Supplier<Entreprise> getEntrepriseByRegpmIdSupplier(IdMapper idMapper, long id) {
		return () -> getEntityFromDb(Entreprise.class, idMapper.getIdUniregEntreprise(id));
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByUniregIdSupplier(long id) {
		return () -> getEntityFromDb(Etablissement.class, id);
	}

	@NotNull
	protected final Supplier<Etablissement> getEtablissementByRegpmIdSupplier(IdMapper idMapper, long id) {
		return () -> getEntityFromDb(Etablissement.class, idMapper.getIdUniregEtablissement(id));
	}

	@NotNull
	protected final Supplier<PersonnePhysique> getIndividuByRegpmIdSupplier(IdMapper idMapper, long id) {
		return () -> getEntityFromDb(PersonnePhysique.class, idMapper.getIdUniregIndividu(id));
	}

	/**
	 * @param clazz la classe de l'entité à récupérer depuis la base de données Unireg
	 * @param id identifiant de l'entité à récupérer
	 * @param <E> type de l'entité récupérée
	 * @return l'entité avec l'identifiant donné
	 */
	@Nullable
	protected final <E extends HibernateEntity> E getEntityFromDb(Class<E> clazz, long id) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().get(clazz, id);
	}

	/**
	 * @param entity l'entité à sauvegarder en base de données Unireg
	 * @param <E> type de l'entité
	 * @return l'entité après sauvegarde
	 */
	protected final <E extends HibernateEntity> E saveEntityToDb(E entity) {
		//noinspection unchecked
		return (E) uniregSessionFactory.getCurrentSession().merge(entity);
	}

	/**
	 * Réel job de migration
	 * @param entity entité à migrer
	 * @param mr récipiendaire de messages à logguer
	 * @param linkCollector collecteur de liens entre entités (seront résolus à la fin de la migration)
	 * @param idMapper mapper d'identifiants RegPM -> Unireg
	 */
	protected abstract void doMigrate(T entity, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper);

	/**
	 * Renvoie l'éventuel préfixe à utiliser pour tous les messages enregistrés dans les MigrationResults
	 * @param entity entité en cours de migration
	 * @return le préfixe pour cette entité
	 */
	@Nullable
	protected abstract String getMessagePrefix(T entity);
}
