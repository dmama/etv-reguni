package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;

public class LocationsExtractor implements Function<Organisation, Stream<? extends BigInteger>> {

	@Override
	public Stream<? extends BigInteger> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.map(OrganisationLocation::getCantonalId);
	}
}