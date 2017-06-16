package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.Organisation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container.Keyed;

public class LocationLegalFormExtractor implements Function<Organisation, Stream<Keyed<BigInteger, LegalForm>>> {

	@Override
	public Stream<Keyed<BigInteger, LegalForm>> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> ol.getLegalForm() != null && ol.getLegalForm() != null)
				.map(ol -> new Keyed<>(ol.getCantonalId(), ol.getLegalForm()));
	}
}
