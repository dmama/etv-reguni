package ch.vd.uniregctb.supergra.delta;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.Property;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;
import ch.vd.uniregctb.supergra.SuperGraContext;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Ajoute une sous-entité dans une collection d'une entité parente dans le mode SuperGra.
 */
public class AddSubEntity extends Delta {

	/**
	 * La clé de l'entité parente sur laquelle on ajoute une sous-entité dans une collection
	 */
	private final EntityKey key;

	/**
	 * Le nom de la collection sur l'entité parente
	 */
	private final String collName;

	/**
	 * La classe de la sous-entité à ajouter
	 */
	private final Class subClass;

	/**
	 * L'id de la sous-entité à ajouter
	 */
	private final Long id;

	public AddSubEntity(EntityKey key, String collName, Class subClass, Long id) {
		this.key = key;
		this.collName = collName;
		this.subClass = subClass;
		this.id = id;
	}

	/**
	 * @return La clé de l'entité parente sur laquelle on ajoute une sous-entité dans une collection
	 */
	@Override
	public EntityKey getKey() {
		return key;
	}

	/**
	 * @return Le nom de la collection sur l'entité parente
	 */
	public String getCollName() {
		return collName;
	}

	/**
	 * @return La classe de la sous-entité à ajouter
	 */
	public Class getSubClass() {
		return subClass;
	}

	/**
	 * @return L'id de la sous-entité à ajouter
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return la clé de la sous-entité créée par ce delta.
	 */
	public EntityKey getSubKey() {
		return new EntityKey(EntityType.fromHibernateClass(subClass), id);
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public void apply(HibernateEntity entity, SuperGraContext context) {
		try {
			final PropertyDescriptor desc = new PropertyDescriptor(collName, entity.getClass());
			final Method getter = desc.getReadMethod();

			// Récupère la collection sur le parent
			Set set = (Set) getter.invoke(entity);
			if (set == null) {
				set = new HashSet<>();
				final Method setter = desc.getWriteMethod();
				setter.invoke(entity, set);
			}

			// Récupère le nom de la propriété 'id'
			final MetaEntity meta = MetaEntity.determine(subClass);
			String idProp = null;
			Property parentProp = null;
			for (Property p : meta.getProperties()) {
				if (p.isPrimaryKey()) {
					idProp = p.getName();
				}
				else if (p.isParentForeignKey()) {
					parentProp = p;
				}
			}
			Assert.notNull(idProp);

			// Crée la nouvelle entité
			final EntityKey subKey = new EntityKey(EntityType.fromHibernateClass(subClass), id);
			final HibernateEntity subEntity = context.newEntity(subKey, subClass);
			Assert.notNull(subEntity);

			// Renseigne l'id
			final PropertyDescriptor idDescr = new PropertyDescriptor(idProp, subClass);
			final Method idSetter = idDescr.getWriteMethod();
			idSetter.invoke(subEntity, id);

			// Renseigne le parent
			boolean isRapport = false;
			if (parentProp != null) { // les rapports-entre-tiers ne possèdent pas de parent
				final PropertyDescriptor parentDescr = new PropertyDescriptor(parentProp.getName(), subClass);
				final Method parentSetter = parentDescr.getWriteMethod();
				parentSetter.invoke(subEntity, entity);
			}
			else if (subEntity instanceof RapportEntreTiers) {
				isRapport = true;
				// cas spécial des rapports entre tiers où le lien doit être fait à la main
				final RapportEntreTiers r = (RapportEntreTiers) subEntity;
				if (collName.equals("rapportsSujet")) {
					r.setSujetId(((Tiers) entity).getId());
				}
				else if (collName.equals("rapportsObjet")) {
					r.setObjetId(((Tiers) entity).getId());
				}
			}

			// Ajoute l'entité à son parent
			if (isRapport && context.isForCommit()) {
				// [UNIREG-3160] lorsqu'on ajoute un rapport-entre-tiers dans le but de sauver les changements dans le base, on évite de l'ajouter à la collection du parent.
				// Autrement hibernate se retrouve avec une entité transiente dans une collection (rapportsObjet ou rapportsSujet) qui n'est pas responsable des éléments (selon
				// les annotations utilisées, les rapports-entre-tiers pointent vers leurs objet/sujet mais ils ne leur appartiennent pas) et il lève une TransientObjectException.
				// A la place, on le met-de-côté pour être sauvé lorsque les liens vers les objet/sujet seront correctement établis.
				context.scheduleForSave((RapportEntreTiers) subEntity);
			}
			else {
				set.add(subEntity);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getHtml() {
		return "Ajout du " + subClass.getSimpleName() + " n°" + id + " dans la collection " + attribute2html(collName) + " du " + key;
	}
}
