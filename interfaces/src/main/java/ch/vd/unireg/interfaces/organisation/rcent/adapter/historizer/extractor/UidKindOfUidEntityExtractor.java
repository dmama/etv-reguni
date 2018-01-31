package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.KindOfUidEntity;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class UidKindOfUidEntityExtractor implements Function<Organisation, Stream<Keyed<BigInteger, KindOfUidEntity>>> {

	@Override
	public Stream<Keyed<BigInteger, KindOfUidEntity>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getUidRegisterData() != null && ol.getUidRegisterData().getKindOfUidEntity() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getUidRegisterData().getKindOfUidEntity()));
	}
}