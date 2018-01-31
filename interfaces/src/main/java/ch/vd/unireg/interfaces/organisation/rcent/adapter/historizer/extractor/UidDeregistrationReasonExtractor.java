package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.Organisation;
import ch.vd.evd0022.v3.UidDeregistrationReason;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class UidDeregistrationReasonExtractor implements Function<Organisation, Stream<Keyed<BigInteger, UidDeregistrationReason>>> {

	@Override
	public Stream<Keyed<BigInteger, UidDeregistrationReason>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getDeregistrationReason() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getDeregistrationReason()));
	}
}