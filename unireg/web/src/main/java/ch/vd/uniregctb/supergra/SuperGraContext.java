package ch.vd.uniregctb.supergra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.validation.ValidationInterceptor;

/**
 * Context d'exécution Hibernate du mode SuperGra. Ce context contient toutes les entités nouvelles créées (en plus des entités déjà cachées dans la session Hibernate).
 */
public class SuperGraContext {

	private final Session session;
	private boolean forCommit;
	private final ValidationInterceptor validationInterceptor;
	private final Map<EntityKey, HibernateEntity> newlyCreated = new HashMap<EntityKey, HibernateEntity>();
	private final Set<RapportEntreTiers> scheduledForSave = new HashSet<RapportEntreTiers>();

	/**
	 * Crée un context SuperGra associé à une session Hibernate.
	 * <p/>
	 * <b>Note:</b> la durée de vie du context ne doit pas excéder celle de la session.
	 *
	 * @param session               une session Hibernate ouverte et valide.
	 * @param forCommit             <b>vrai</b> si l'appel est effectué dans l'optique de sauver le changement en base de données; <b>faux</b> si l'appel est effectué uniquement dans le but d'afficher le
	 *                              changement.
	 * @param validationInterceptor l'intercepteur de validation
	 */
	public SuperGraContext(Session session, boolean forCommit, ValidationInterceptor validationInterceptor) {
		this.session = session;
		this.forCommit = forCommit;
		this.validationInterceptor = validationInterceptor;

		this.session.setFlushMode(FlushMode.COMMIT);
	}

	/**
	 * @return <b>vrai</b> si l'appel est effectué dans l'optique de sauver le changement en base de données; <b>faux</b> si l'appel est effectué uniquement dans le but d'afficher le changement.
	 */
	public boolean isForCommit() {
		return forCommit;
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

	public boolean isScheduledForSave(RapportEntreTiers rapport) {
		return scheduledForSave.contains(rapport);
	}

	/**
	 * Agende la sauvegarde du rapport spécifié dans la session hibernate.
	 *
	 * @param entity une entité
	 */
	public void scheduleForSave(RapportEntreTiers entity) {
		scheduledForSave.add(entity);
	}

	/**
	 * Applique toutes les actions en attente qui le nécessite.
	 */
	public void finish() {
		for (RapportEntreTiers rapport : scheduledForSave) {
			// [UNIREG-3160] On désactive temporairement la validation parce qu'il est nécessaire de passer par un état momentanément invalide lors de
			// la création d'un rapport. Dans tous les cas, les tiers liés par le rapport seront validés eux-mêmes lors du commit de la transaction
			validationInterceptor.setEnabled(false);
			try {
				rapport = (RapportEntreTiers) session.merge(rapport);
			}
			finally {
				validationInterceptor.setEnabled(true);
			}
			final Tiers sujet = (Tiers) getEntity(new EntityKey(EntityType.Tiers, rapport.getSujetId()));
			sujet.addRapportSujet(rapport);
			final Tiers objet = (Tiers) getEntity(new EntityKey(EntityType.Tiers, rapport.getObjetId()));
			objet.addRapportObjet(rapport);
		}
	}
}
