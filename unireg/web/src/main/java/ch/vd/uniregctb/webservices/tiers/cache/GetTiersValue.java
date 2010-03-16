package ch.vd.uniregctb.webservices.tiers.cache;

import java.util.Set;

import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

class GetTiersValue extends CacheValueWithParts<Tiers> {

	public GetTiersValue(Set<TiersPart> parts, Tiers tiers) {
		super(parts, tiers);
	}

	@Override
	protected void copyParts(Set<TiersPart> parts, Tiers from, Tiers to) {
		to.copyPartsFrom(from, parts);
	}

	@Override
	protected Tiers restrictTo(Tiers tiers, Set<TiersPart> parts) {
		return tiers == null ? null : tiers.clone(parts);
	}

}
