package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.DissolutionReason;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class RcVdDissolutionReasonExtractor implements Function<Organisation, Stream<Keyed<BigInteger, DissolutionReason>>> {

	@Override
	public Stream<Keyed<BigInteger, DissolutionReason>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getVdDissolutionReason() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getVdDissolutionReason()));
	}
}