package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class LocationRcByLawsDateExtractor implements Function<Organisation, Stream<Keyed<BigInteger, RegDate>>> {
	@Override
	public Stream<Keyed<BigInteger, RegDate>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getByLawsDate() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getByLawsDate()));
	}
}
