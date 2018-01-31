package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class LocationFunctionsExtractor implements Function<Organisation, Stream<Keyed<BigInteger, ch.vd.evd0022.v3.Function>>> {
	@Override
	public Stream<Keyed<BigInteger, ch.vd.evd0022.v3.Function>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getFunction() != null)
				.flatMap(this::mapFunctions);
	}

	private Stream<Keyed<BigInteger, ch.vd.evd0022.v3.Function>> mapFunctions(OrganisationLocation ol) {
		return ol.getFunction().stream()
				.map(n -> new Keyed<>(ol.getCantonalId(), n));
	}
}
