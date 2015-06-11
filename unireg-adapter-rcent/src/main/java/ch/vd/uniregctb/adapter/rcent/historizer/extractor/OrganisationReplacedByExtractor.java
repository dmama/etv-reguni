package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;

import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Organisation;

public class OrganisationReplacedByExtractor implements Function<Organisation, BigInteger> {

	@Override
	public BigInteger apply(Organisation org) {
		final Identification replacedBy = org.getReplacedBy();
		return replacedBy != null ? replacedBy.getCantonalId() : null;
	}
}
