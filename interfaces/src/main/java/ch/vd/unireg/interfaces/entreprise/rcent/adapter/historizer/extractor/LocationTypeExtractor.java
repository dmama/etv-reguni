package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.container.Keyed;

public class LocationTypeExtractor implements java.util.function.Function<Organisation, Stream<Keyed<BigInteger, TypeOfLocation>>> {


	@Override
	public Stream<Keyed<BigInteger, TypeOfLocation>> apply(Organisation organisation) {
		return organisation.getOrganisationLocation().stream()
				.filter(ol -> ol.getTypeOfLocation() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getTypeOfLocation()));
	}
}
