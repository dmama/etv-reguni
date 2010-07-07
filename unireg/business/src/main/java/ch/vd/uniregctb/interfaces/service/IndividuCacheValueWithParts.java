package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.interfaces.model.Individu;

import java.util.Set;

/**
 * Donnée cachée par le cache du service civile. Il s'agit de l'individu et des informations nécessaires pour gérer de manière intelligente les différentes parties qui peuvent être renseignées.
 */
public class IndividuCacheValueWithParts extends CacheValueWithParts<Individu, EnumAttributeIndividu> {

	public IndividuCacheValueWithParts(Set<EnumAttributeIndividu> parts, Individu value) {
		super(parts, value);
	}

	@Override
	protected void copyParts(Set<EnumAttributeIndividu> parts, Individu from, Individu to) {
		to.copyPartsFrom(from, parts);
	}

	@Override
	protected Individu restrictTo(Individu value, Set<EnumAttributeIndividu> parts) {
		return value == null ? null : value.clone(parts);
	}
}
