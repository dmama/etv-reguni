package ch.vd.uniregctb.webservices.party4.cache;

import java.util.Set;

import ch.vd.unireg.webservices.party4.PartyPart;
import ch.vd.unireg.xml.party.v2.Party;
import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.webservices.party4.impl.DataHelper;
import ch.vd.uniregctb.xml.party.v2.PartyBuilder;

class GetPartyValue extends CacheValueWithParts<Party, PartyPart> {

	public GetPartyValue(Set<PartyPart> parts, Party tiers) {
		super(parts, tiers);
	}

	@Override
	protected void copyParts(Set<PartyPart> parts, Party from, Party to) {
		PartyBuilder.copyParts(to, from, DataHelper.webToXML(parts));
	}

	@Override
	protected Party restrictTo(Party tiers, Set<PartyPart> parts) {
		return tiers == null ? null : PartyBuilder.clone(tiers, DataHelper.webToXML(parts));
	}

}
