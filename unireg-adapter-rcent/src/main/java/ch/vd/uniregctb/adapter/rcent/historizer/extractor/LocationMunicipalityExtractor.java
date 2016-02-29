package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationMunicipalityExtractor implements Function<Organisation, Stream<Keyed<BigInteger, Integer>>> {

	@Override
	public Stream<Keyed<BigInteger, Integer>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getMunicipality() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getMunicipality().getMunicipalityId()));
	}
}