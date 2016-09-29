package ch.vd.uniregctb.adapter.rcent.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.BurOrganisationLocationData;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;
import ch.vd.uniregctb.adapter.rcent.model.BurRegistrationData;

public class BURRegistrationDataExtractor implements Function<Organisation, Stream<Keyed<BigInteger, BurRegistrationData>>> {

	@Override
	public Stream<Keyed<BigInteger, BurRegistrationData>> apply(Organisation organisation) {
		return organisation.getOrganisationLocation().stream()
				.filter(ol -> ol.getBurRegisterData() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), extract(ol.getBurRegisterData())));
	}

	private static BurRegistrationData extract(BurOrganisationLocationData source) {
		return new BurRegistrationData(source.getRegistrationStatus(), source.getRegistrationDate());
	}
}
