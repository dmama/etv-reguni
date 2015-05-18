package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;

public class AdressesEffectivesIdeExtractor implements Function<Organisation, Stream<Keyed<BigInteger, Address>>> {

	@Override
	public Stream<Keyed<BigInteger, Address>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getEffectiveAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getEffectiveAddress()));
	}
}
