package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.Set;

import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.data.TiersBuilder;

class GetTiersValue extends CacheValueWithParts<Tiers, TiersPart> {

	public GetTiersValue(Set<TiersPart> parts, Tiers tiers) {
		super(parts, tiers);
	}

	@Override
	protected void copyParts(Set<TiersPart> parts, Tiers from, Tiers to) {
		TiersBuilder.copyParts(to, from, parts);
	}

	@Override
	protected Tiers restrictTo(Tiers tiers, Set<TiersPart> parts) {
		return tiers == null ? null : TiersBuilder.clone(tiers, parts);
	}

}
