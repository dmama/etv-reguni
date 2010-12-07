package ch.vd.uniregctb.supergra.delta;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.SuperGraContext;

/**
 * Met-à-jour la valeur d'un attribut sur une entité dans le mode SuperGra.
 */
public class AttributeUpdate extends Delta {

	private EntityKey key;
	private String name;
	private Object oldValue;
	private Object newValue;

	public AttributeUpdate(EntityKey key, String name, Object oldValue, Object newValue) {
		this.key = key;
		this.name = name;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public EntityKey getKey() {
		return key;
	}

	public void setKey(EntityKey key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public void setOldValue(Object oldValue) {
		this.oldValue = oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	public void setNewValue(Object newValue) {
		this.newValue = newValue;
	}

	@Override
	public String getHtml() {
		if (oldValue == null && newValue != null) {
			return "Renseignement du champ " + attribute2html(name) + " à " + value2html(newValue) + " sur le " + key;
		}
		else if (oldValue != null && newValue == null) {
			return "Mise-à-nul du champ " + attribute2html(name) + " sur le " + key;
		}
		else {
			return "Mise-à-jour du champ " + attribute2html(name) + " de " + value2html(oldValue) + " à " + value2html(newValue) + " sur le " + key;
		}
	}

	@Override
	public void apply(HibernateEntity entity, SuperGraContext context) {
		try {
			PropertyDescriptor descr = new PropertyDescriptor(name, entity.getClass());
			Method setter = descr.getWriteMethod();

			if (newValue instanceof EntityKey) {
				if (Number.class.isAssignableFrom(descr.getPropertyType())) {
					// cas spécial du lien vers une entité gérée à la main (pour les rapport-entre-tiers, par exemple) : on doit travailler directement avec les ids.
					setter.invoke(entity, ((EntityKey) newValue).getId());
				}
				else {
					// dans le cas d'une entity key, il faut aller chercher l'entité hibernate elle-même et l'assigner.
					HibernateEntity newEntity = context.getEntity((EntityKey) newValue);
					setter.invoke(entity, newEntity);
				}
			}
			else {
				setter.invoke(entity, newValue);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
