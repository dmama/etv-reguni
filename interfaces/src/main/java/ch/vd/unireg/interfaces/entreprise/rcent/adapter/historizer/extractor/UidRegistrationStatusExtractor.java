package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.container.Keyed;

public class UidRegistrationStatusExtractor implements Function<Organisation, Stream<Keyed<BigInteger, UidRegisterStatus>>> {

	@Override
	public Stream<Keyed<BigInteger, UidRegisterStatus>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getRegistrationStatus() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getRegistrationStatus()));
	}
}