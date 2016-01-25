package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationInReplacementOfExtractor implements Function<Organisation, Stream<Keyed<BigInteger, BigInteger>>> {
	@Override
	public Stream<Keyed<BigInteger, BigInteger>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getInReplacementOf() != null)
				.flatMap(this::mapNames);
	}

	private Stream<Keyed<BigInteger, BigInteger>> mapNames(OrganisationLocation ol) {
		return ol.getInReplacementOf().stream().map(id -> new Keyed<>(ol.getCantonalId(), id.getCantonalId()));
	}
}
