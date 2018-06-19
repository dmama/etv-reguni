package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.container.Keyed;

public class LocationRcPurposeExtractor implements Function<Organisation, Stream<Keyed<BigInteger, String>>> {
	@Override
	public Stream<Keyed<BigInteger, String>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getPurpose() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getPurpose()));
	}
}
