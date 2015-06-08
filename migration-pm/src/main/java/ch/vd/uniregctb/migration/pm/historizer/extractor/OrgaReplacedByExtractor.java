package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;

import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Organisation;

public class OrgaReplacedByExtractor implements Function<Organisation, BigInteger> {

	@Override
	public BigInteger apply(Organisation org) {
		final Identification replacedBy = org.getReplacedBy();
		return replacedBy != null ? replacedBy.getCantonalId() : null;
	}
}
