package ch.vd.uniregctb.interfaces.service;

import java.io.Serializable;
import java.util.Set;

import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;

/**
 * Donnée cachée par le cache du service personne morale. Il s'agit de la personne morale et des informations nécessaires pour gérer de manière intelligente les différentes parties qui peuvent être
 * renseignées.
 */
public class PersonneMoraleCacheValueWithParts extends CacheValueWithParts<PersonneMorale, PartPM> implements Serializable {

	private static final long serialVersionUID = 0L;

	public PersonneMoraleCacheValueWithParts(Set<PartPM> parts, PersonneMorale value) {
		super(parts, value);
	}

	@Override
	protected void copyParts(Set<PartPM> parts, PersonneMorale from, PersonneMorale to) {
		to.copyPartsFrom(from, parts);
	}

	@Override
	protected PersonneMorale restrictTo(PersonneMorale value, Set<PartPM> parts) {
		return value == null ? null : value.clone(parts);
	}
}
