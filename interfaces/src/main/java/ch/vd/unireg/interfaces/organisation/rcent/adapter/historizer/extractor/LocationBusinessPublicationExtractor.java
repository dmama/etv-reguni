package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.BusinessPublication;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class LocationBusinessPublicationExtractor implements Function<Organisation, Stream<Keyed<BigInteger, BusinessPublication>>> {
	@Override
	public Stream<Keyed<BigInteger, BusinessPublication>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getBusinessPublication() != null)
				.flatMap(this::mapFunctions);
	}

	private Stream<Keyed<BigInteger, BusinessPublication>> mapFunctions(OrganisationLocation ol) {
		return ol.getBusinessPublication().stream()
				.map(n -> new Keyed<>(ol.getCantonalId(), n));
	}
}
