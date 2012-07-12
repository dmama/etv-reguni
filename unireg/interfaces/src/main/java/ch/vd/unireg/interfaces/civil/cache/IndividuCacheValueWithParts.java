package ch.vd.unireg.interfaces.civil.cache;

import java.io.Serializable;
import java.util.Set;

import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.cache.CacheValueWithParts;

/**
 * Donnée cachée par le cache du service civile. Il s'agit de l'individu et des informations nécessaires pour gérer de manière intelligente les différentes parties qui peuvent être renseignées.
 */
public class IndividuCacheValueWithParts extends CacheValueWithParts<Individu, AttributeIndividu> implements Serializable {

	private static final long serialVersionUID = -6901410842345964808L;

	public IndividuCacheValueWithParts(Set<AttributeIndividu> parts, Individu value) {
		super(parts, value);
	}

	@Override
	protected void copyParts(Set<AttributeIndividu> parts, Individu from, Individu to) {
		to.copyPartsFrom(from, parts);
	}

	@Override
	protected Individu restrictTo(Individu value, Set<AttributeIndividu> parts) {
		return value == null ? null : value.clone(parts);
	}
}
