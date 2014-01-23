package ch.vd.uniregctb.webservices.v5.cache;

import java.io.Serializable;
import java.util.Set;

import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.uniregctb.cache.CacheValueWithParts;
import ch.vd.uniregctb.xml.party.v3.PartyBuilder;

/**
 * Container d'une valeur mise en cache pour un tiers sorti d'un GetParty
 */
final class GetPartyValue extends CacheValueWithParts<Party, PartyPart> implements Serializable {

	private static final long serialVersionUID = 7220905179755973880L;

	GetPartyValue(Set<PartyPart> parts, Party value) {
		super(PartyPart.class, parts, value);
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
