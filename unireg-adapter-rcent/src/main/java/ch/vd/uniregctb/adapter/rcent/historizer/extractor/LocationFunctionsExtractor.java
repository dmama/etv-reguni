package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationFunctionsExtractor implements Function<Organisation, Stream<Keyed<BigInteger, ch.vd.evd0022.v1.Function>>> {
	@Override
	public Stream<Keyed<BigInteger, ch.vd.evd0022.v1.Function>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.flatMap(this::mapFunctions);
	}

	private Stream<Keyed<BigInteger, ch.vd.evd0022.v1.Function>> mapFunctions(OrganisationLocation ol) {
		return ol.getFunction().stream().map(n -> new Keyed<>(ol.getCantonalId(), n));
	}
}
