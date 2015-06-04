package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;

public class AdressesLegalesExtractor implements Function<Organisation, Stream<Keyed<BigInteger, Address>>> {

	@Override
	public Stream<Keyed<BigInteger, Address>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getLegalAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getLegalAddress()));
	}
}