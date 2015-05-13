package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.extractor.Extractor;

public class AdressesLegalesExtractor implements Extractor<Organisation, Stream<Keyed<BigInteger, Address>>> {

	@Nullable
	@Override
	public Stream<Keyed<BigInteger, Address>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null && ol.getCommercialRegisterData().getLegalAddress() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getCommercialRegisterData().getLegalAddress()));
	}

}