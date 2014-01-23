package ch.vd.uniregctb.webservices.party3.cache;

import java.io.Serializable;
import java.util.Set;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.xml.party.v1.PartyBuilder;

class GetPartyValue extends CacheValueWithParts<Party, PartyPart> implements Serializable {

	private static final long serialVersionUID = 5895901475025746422L;

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
