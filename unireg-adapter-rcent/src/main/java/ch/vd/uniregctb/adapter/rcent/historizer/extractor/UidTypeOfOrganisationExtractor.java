package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.UidRegisterTypeOfOrganisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;

public class UidTypeOfOrganisationExtractor implements Function<Organisation, Stream<Keyed<BigInteger, UidRegisterTypeOfOrganisation>>> {

	@Override
	public Stream<Keyed<BigInteger, UidRegisterTypeOfOrganisation>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getTypeOfOrganisation() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getTypeOfOrganisation()));
	}
}