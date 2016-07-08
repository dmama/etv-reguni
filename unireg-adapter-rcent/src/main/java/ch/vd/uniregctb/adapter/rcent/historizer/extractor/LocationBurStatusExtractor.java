package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.BurLocalUnitStatus;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationBurStatusExtractor implements Function<Organisation, Stream<Keyed<BigInteger, BurLocalUnitStatus>>> {

	@Override
	public Stream<Keyed<BigInteger, BurLocalUnitStatus>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getBurRegisterData() != null && ol.getBurRegisterData().getRegistrationStatus() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getBurRegisterData().getRegistrationStatus()));
	}
}