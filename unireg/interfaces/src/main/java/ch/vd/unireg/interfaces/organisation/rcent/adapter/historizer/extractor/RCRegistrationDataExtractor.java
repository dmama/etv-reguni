package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.CommercialRegisterData;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.RCRegistrationData;

public class RCRegistrationDataExtractor implements Function<Organisation, Stream<Keyed<BigInteger, RCRegistrationData>>> {

	@Override
	public Stream<Keyed<BigInteger, RCRegistrationData>> apply(Organisation organisation) {
		return organisation.getOrganisationLocation().stream()
				.filter(ol -> ol.getCommercialRegisterData() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), extract(ol.getCommercialRegisterData())));
	}

	private static RCRegistrationData extract(CommercialRegisterData source) {
		return new RCRegistrationData(source.getRegistrationStatus(), source.getRegistrationDate(), source.getDeregistrationDate(),
		                              source.getVdRegistrationDate(), source.getVdDeregistrationDate(), source.getVdDissolutionReason());
	}
}
