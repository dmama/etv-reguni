package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class CapitalExtractor implements Function<Organisation, Stream<Keyed<BigInteger, Capital>>> {

	@Override
	public Stream<Keyed<BigInteger, Capital>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getCapital() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getCapital()));
	}
}