package ch.vd.uniregctb.webservices.tiers2.cache;

import java.util.Set;

import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

class GetTiersHistoValue extends CacheValueWithParts<TiersHisto> {

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
