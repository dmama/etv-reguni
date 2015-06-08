package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;

public class LocationOtherNamesExtractor implements Function<Organisation, Stream<Keyed<BigInteger, String>>> {
	@Override
	public Stream<Keyed<BigInteger, String>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.flatMap(this::mapNames);
	}

	private Stream<Keyed<BigInteger, String>> mapNames(OrganisationLocation ol) {
		return ol.getOtherName().stream().map(n -> new Keyed<>(ol.getCantonalId(), n));
	}
}
