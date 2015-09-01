package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class LocationRcEntryDateExtractor implements Function<Organisation, Stream<Keyed<BigInteger, RegDate>>> {
	@Override
	public Stream<Keyed<BigInteger, RegDate>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getEntryDate()));
	}
}
