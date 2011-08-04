package ch.vd.uniregctb.supergra.delta;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.supergra.EntityKey;
import ch.vd.uniregctb.supergra.EntityType;
import ch.vd.uniregctb.supergra.SuperGraContext;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;

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

	@Override
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
					// [UNIREG-3160] cas spécial du lien vers une entité gérée à la main (pour les rapport-entre-tiers, par exemple) : on doit travailler directement avec les ids.
					if (entity instanceof RapportEntreTiers) {
						// cas spécial des rapport-entre-tiers
						applyRapportUpdate((RapportEntreTiers) entity, context, setter);
					}
					else {
						final Long id = ((EntityKey) newValue).getId();
						setter.invoke(entity, id);
					}
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

	private void applyRapportUpdate(RapportEntreTiers rapport, SuperGraContext context, Method setter) throws IllegalAccessException, InvocationTargetException {

		final Long id = ((EntityKey) newValue).getId();

		if (context.isForCommit() && context.isScheduledForSave(rapport)) {
			// [UNIREG-3160] lorsqu'on ajoute un rapport-entre-tiers dans le but de le sauver pour la première fois dans la base (à la place de simplement les afficher),
			// on se contente de mettre-à-jour la propriété.  On sauvegardera la rapport dans la méthode 'finish' du context supergra, lorsque les propriétés
			// sujetId et objetId seront renseignées.
			setter.invoke(rapport, id);
		}
		else {
			Set<RapportEntreTiers> oldSet = null;
			Set<RapportEntreTiers> newSet = null;

			if ("sujetId".equals(name)) {
				final Long sujetId = rapport.getSujetId();
				if (sujetId != null) {
					Tiers sujet = (Tiers) context.getEntity(new EntityKey(EntityType.Tiers, sujetId));
					if (sujet != null) {
						oldSet = sujet.getRapportsSujet();
					}
				}
				if (id != null) {
					Tiers sujet = (Tiers) context.getEntity(new EntityKey(EntityType.Tiers, id));
					if (sujet != null) {
						newSet = sujet.getRapportsSujet();
					}
				}
			}
			else if ("objetId".equals(name)) {
				final Long objetId = rapport.getObjetId();
				if (objetId != null) {
					Tiers objet = (Tiers) context.getEntity(new EntityKey(EntityType.Tiers, objetId));
					if (objet != null) {
						oldSet = objet.getRapportsObjet();
					}
				}
				if (id != null) {
					Tiers objet = (Tiers) context.getEntity(new EntityKey(EntityType.Tiers, id));
					if (objet != null) {
						newSet = objet.getRapportsObjet();
					}
				}
			}
			
			// [UNIREG-3160] Dans le cas d'un rapport-entre-tiers, on doit mettre-à-jour les collections rapportsObjet et
			// rapportsSujet à la main, pour que la validation dans la GUI travaille sur des données cohérentes.
			if (oldSet != null) {
				oldSet.remove(rapport);
			}

			setter.invoke(rapport, id);

			if (newSet != null) {
				newSet.add(rapport);
			}
		}
	}
}
