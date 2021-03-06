package ch.vd.unireg.supergra;

import javax.persistence.FlushModeType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.dataimport.processor.CommunauteRFProcessor;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.validation.ValidationInterceptor;

/**
 * Context d'exécution Hibernate du mode SuperGra. Ce context contient toutes les entités nouvelles créées (en plus des entités déjà cachées dans la session Hibernate).
 */
public class SuperGraContext {

	private final Session session;
	private final boolean forCommit;
	private final ValidationInterceptor validationInterceptor;
	private final CommunauteRFProcessor communauteRFProcessor;

	private final Map<EntityKey, HibernateEntity> newlyCreated = new HashMap<>();
	private final Set<HibernateEntity> scheduledForSave = new HashSet<>();

	/**
	 * Crée un context SuperGra associé à une session Hibernate.
	 * <p/>
	 * <b>Note:</b> la durée de vie du context ne doit pas excéder celle de la session.
	 *  @param session               une session Hibernate ouverte et valide.
	 * @param forCommit             <b>vrai</b> si l'appel est effectué dans l'optique de sauver le changement en base de données; <b>faux</b> si l'appel est effectué uniquement dans le but d'afficher le
	 *                              changement.
	 * @param validationInterceptor l'intercepteur de validation
	 * @param communauteRFProcessor le processeur des communautés RF
	 */
	public SuperGraContext(Session session, boolean forCommit, ValidationInterceptor validationInterceptor, CommunauteRFProcessor communauteRFProcessor) {
		this.session = session;
		this.forCommit = forCommit;
		this.validationInterceptor = validationInterceptor;
		this.communauteRFProcessor = communauteRFProcessor;

		this.session.setFlushMode(FlushModeType.COMMIT);
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
			entity = clazz.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (entity == null) {
			throw new IllegalArgumentException();
		}

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

	public boolean isNewlyCreated(EntityKey key) {
		return newlyCreated.containsKey(key);
	}

	public boolean isScheduledForSave(RapportEntreTiers rapport) {
		return scheduledForSave.contains(rapport);
	}

	/**
	 * Agende la sauvegarde du rapport spécifié dans la session hibernate.
	 *
	 * @param entity une entité
	 */
	public void scheduleForSave(HibernateEntity entity) {
		scheduledForSave.add(entity);
	}

	/**
	 * Recalcul les regroupements de la communaute RF spécifiée.
	 *
	 * @param communauteRF une communauté RF
	 */
	public void recalculeRegroupements(@NotNull CommunauteRF communauteRF) {
		communauteRFProcessor.process(communauteRF);
	}

	/**
	 * Applique toutes les actions en attente qui le nécessite.
	 */
	public void finish() {
		for (HibernateEntity entity : scheduledForSave) {
			if (entity instanceof RapportEntreTiers) {
				RapportEntreTiers rapport = (RapportEntreTiers) entity;
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
			else {
				session.merge(entity);
			}
		}
	}
}
