package ch.vd.uniregctb.supergra;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.meta.MetaEntity;
import ch.vd.uniregctb.hibernate.meta.Property;

/**
 * Ajoute une sous-entité dans une collection d'une entité parente dans le mode SuperGra.
 */
public class AddSubEntity extends Delta {

	/**
	 * La clé de l'entité parente sur laquelle on ajoute une sous-entité dans une collection
	 */
	private EntityKey key;

	/**
	 * Le nom de la collection sur l'entité parente
	 */
	private String collName;

	/**
	 * La classe de la sous-entité à ajouter
	 */
	private Class subClass;

	/**
	 * L'id de la sous-entité à ajouter
	 */
	private Long id;

	public AddSubEntity(EntityKey key, String collName, Class subClass, Long id) {
		this.key = key;
		this.collName = collName;
		this.subClass = subClass;
		this.id = id;
	}

	@Override
	public EntityKey getKey() {
		return key;
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
				set = new HashSet<Object>();
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
			if (parentProp != null) { // les rapports-entre-tiers ne possèdent pas de parent
				final PropertyDescriptor parentDescr = new PropertyDescriptor(parentProp.getName(), subClass);
				final Method parentSetter = parentDescr.getWriteMethod();
				parentSetter.invoke(subEntity, entity);
			}

			// Ajoute l'entité à son parent
			set.add(subEntity);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "Ajout du " + subClass.getSimpleName() + " n°" + id + " dans la collection " + collName + " du " + key;
	}
}
