package ch.vd.uniregctb.migration.pm.historizer.extractor;

import java.math.BigInteger;
import java.util.function.Function;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.evd0022.v1.OrganisationLocation;

public class EtablissementPrincipalExtractor implements Function<Organisation, BigInteger> {

	@Override
	public BigInteger apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_PRINCIPAL == ol.getKindOfLocation())
				.map(OrganisationLocation::getCantonalId)
				.findAny()
				.orElse(null);
	}

}