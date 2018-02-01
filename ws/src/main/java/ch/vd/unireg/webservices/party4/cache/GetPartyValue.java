package ch.vd.unireg.webservices.party4.cache;

import java.io.Serializable;
import java.util.Set;

import ch.vd.unireg.webservices.party4.PartyPart;
import ch.vd.unireg.xml.party.v2.Party;
import ch.vd.unireg.cache.CacheValueWithParts;
import ch.vd.unireg.webservices.party4.impl.DataHelper;
import ch.vd.unireg.xml.party.v2.PartyBuilder;

class GetPartyValue extends CacheValueWithParts<Party, PartyPart> implements Serializable {

	private static final long serialVersionUID = -5109367985727902846L;

	public GetPartyValue(Set<PartyPart> parts, Party tiers) {
		super(PartyPart.class, parts, tiers);
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
