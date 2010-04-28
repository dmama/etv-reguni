package ch.vd.uniregctb.webservices.tiers.cache;

import java.util.Set;

import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.webservices.tiers.TiersHisto;
import ch.vd.uniregctb.webservices.tiers.TiersPart;

class GetTiersHistoValue extends CacheValueWithParts<TiersHisto, TiersPart> {

	public GetTiersHistoValue(Set<TiersPart> parts, TiersHisto tiers) {
		super(parts, tiers);
	}

	@Override
	protected void copyParts(Set<TiersPart> parts, TiersHisto from, TiersHisto to) {
		to.copyPartsFrom(from, parts);
	}

	@Override
	protected TiersHisto restrictTo(TiersHisto tiers, Set<TiersPart> parts) {
		return tiers == null ? null : tiers.clone(parts);
	}

}
