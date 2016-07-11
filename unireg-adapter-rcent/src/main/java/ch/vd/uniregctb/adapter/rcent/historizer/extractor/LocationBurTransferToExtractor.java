package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationBurTransferToExtractor implements Function<Organisation, Stream<Keyed<BigInteger, BigInteger>>> {

	@Override
	public Stream<Keyed<BigInteger, BigInteger>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getBurTransferTo() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getBurTransferTo().getCantonalId()));
	}
}
