package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class AdressesEffectivesIdeExtractor implements Function<Organisation, Stream<Keyed<BigInteger, Address>>> {

	@Override
	public Stream<Keyed<BigInteger, Address>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getAddresses() != null && ol.getAddresses().getEffectiveAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getAddresses().getEffectiveAddress()));
	}
}
