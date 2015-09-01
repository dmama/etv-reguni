package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationIdentifiersExtractor implements Function<Organisation, Stream<Keyed<BigInteger, Identifier>>> {

	@Override
	public Stream<Keyed<BigInteger, Identifier>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.flatMap(LocationIdentifiersExtractor::mapNames);
	}

	private static Stream<Keyed<BigInteger, Identifier>> mapNames(OrganisationLocation ol) {
		return ol.getIdentifier().stream().map(id -> new Keyed<>(ol.getCantonalId(), id));
	}
}
