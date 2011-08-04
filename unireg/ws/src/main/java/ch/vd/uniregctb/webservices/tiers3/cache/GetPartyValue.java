package ch.vd.uniregctb.webservices.tiers3.cache;

import java.util.Set;

import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.webservices.tiers3.data.PartyBuilder;

class GetPartyValue extends CacheValueWithParts<Party, PartyPart> {

	public GetPartyValue(Set<PartyPart> parts, Party tiers) {
		super(parts, tiers);
	}

	@Override
	protected void copyParts(Set<PartyPart> parts, Party from, Party to) {
		PartyBuilder.copyParts(to, from, parts);
	}

	@Override
	protected Party restrictTo(Party tiers, Set<PartyPart> parts) {
		return tiers == null ? null : PartyBuilder.clone(tiers, parts);
	}

}
