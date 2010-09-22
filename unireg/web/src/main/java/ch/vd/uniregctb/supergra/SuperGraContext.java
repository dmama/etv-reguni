package ch.vd.uniregctb.supergra;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Context d'exécution Hibernate du mode SuperGra. Ce context contient toutes les entités nouvelles créées (en plus des entités déjà cachées dans la session Hibernate).
 */
public class SuperGraContext {

	private final Session session;
	private final Map<EntityKey, HibernateEntity> newlyCreated = new HashMap<EntityKey, HibernateEntity>();

	/**
	 * Crée un context SuperGra associé à une session Hibernate.
	 * <p/>
	 * <b>Note:</b> la durée de vie du context ne doit pas excéder celle de la session.
	 *
	 * @param session une session Hibernate ouverte et valide.
	 */
	public SuperGraContext(Session session) {
		this.session = session;
	}

	/**
	 * Créée et enregistre dans le cache une nouvelle entité.
	 *
	 * @param key   la clé de la nouvelle entité à créer
	 * @param clazz la classe concrete de l'entité à créer
	 * @return une nouvelle entité
	 */
	public HibernateEntity newEntity(EntityKey key, Class<? extends HibernateEntity> clazz) {

		// Crée la nouvelle entité
		final HibernateEntity entity;
		try {
			entity = clazz.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		Assert.notNull(entity);

		// Enregistre la nouvelle entité
		newlyCreated.put(key, entity);

		return entity;
	}

	/**
	 * Charge une entité hibernate à partir de sa clé. Cette méthode cherche d'abord dans le cache des entités nouvellements créées (mais pas encore enregistrées dans la session Hibernate), puis dans la
	 * session Hibernate.
	 *
	 * @param key la clé de l'entité à charger
	 * @return l'entité correspondant à la clé; ou <b>null</b> si cette entité n'est pas trouvée.
	 */
	public HibernateEntity getEntity(EntityKey key) {
		HibernateEntity entity = newlyCreated.get(key);
		if (entity == null) {
			final Class<?> clazz = key.getType().getHibernateClass();
			entity = (HibernateEntity) session.get(clazz, key.getId());
		}
		return entity;
	}
}
