package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.ech.ech0097.v2.NamedOrganisationId;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class LocationIdentifiersExtractor implements Function<Organisation, Stream<Keyed<BigInteger, NamedOrganisationId>>> {

	@Override
	public Stream<Keyed<BigInteger, NamedOrganisationId>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getIdentifier() != null)
				.flatMap(LocationIdentifiersExtractor::mapNames);
	}

	private static Stream<Keyed<BigInteger, NamedOrganisationId>> mapNames(OrganisationLocation ol) {
		return ol.getIdentifier().stream().map(id -> new Keyed<>(ol.getCantonalId(), id));
	}
}
