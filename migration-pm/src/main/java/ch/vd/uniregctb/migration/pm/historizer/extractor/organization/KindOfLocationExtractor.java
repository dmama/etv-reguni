package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.math.BigInteger;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;

public class KindOfLocationExtractor implements java.util.function.Function<Organisation, Stream<Keyed<BigInteger, KindOfLocation>>> {


	@Override
	public Stream<Keyed<BigInteger, KindOfLocation>> apply(Organisation organisation) {
		return organisation.getOrganisationLocation().stream()
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getKindOfLocation()));
	}
}
