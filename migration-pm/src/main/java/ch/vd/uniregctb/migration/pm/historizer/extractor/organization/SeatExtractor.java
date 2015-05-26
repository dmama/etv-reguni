package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;

public class SeatExtractor implements Function<Organisation, Stream<Keyed<BigInteger, SwissMunicipality>>> {

	@Override
	public Stream<Keyed<BigInteger, SwissMunicipality>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getSeat() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getSeat()));
	}
}