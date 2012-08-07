package ch.vd.uniregctb.webservices.party3.cache;

import java.util.Set;

import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.xml.party.PartyBuilder;

class GetPartyValue extends CacheValueWithParts<Party, PartyPart> {

	public GetPartyValue(Set<PartyPart> parts, Party tiers) {
		super(parts, tiers);
	}

	@Override
	protected void copyParts(Set<PartyPart> parts, Party from, Party to) {
		PartyBuilder.copyParts(to, from, DataHelper.webToXML(parts)); // TODO (msi) essayer de fusionner les deux enums Parts au niveau XML, pour éviter d'avoir à les traduires à chaque appel
	}

	@Override
	protected Party restrictTo(Party tiers, Set<PartyPart> parts) {
		return tiers == null ? null : PartyBuilder.clone(tiers, DataHelper.webToXML(parts));
	}

}
