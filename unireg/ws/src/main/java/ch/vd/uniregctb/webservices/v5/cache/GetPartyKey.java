package ch.vd.uniregctb.webservices.v5.cache;

import org.apache.commons.lang3.StringUtils;

final class GetPartyKey extends PartyCacheKey {

	public GetPartyKey(long partyNo) {
		super(partyNo);
	}

	@Override
	protected String toStringPart() {
		return StringUtils.EMPTY;
	}
}
