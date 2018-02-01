package ch.vd.unireg.webservices.v7.cache;

import org.apache.commons.lang3.StringUtils;

public class GetCommunityOfHeirsKey extends PartyCacheKey {

	public GetCommunityOfHeirsKey(int deceasedId) {
		super(deceasedId);
	}

	@Override
	protected String toStringPart() {
		return StringUtils.EMPTY;
	}
}
