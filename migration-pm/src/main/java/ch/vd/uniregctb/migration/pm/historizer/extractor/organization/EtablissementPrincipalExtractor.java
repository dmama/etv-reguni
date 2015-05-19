package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.util.function.Function;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;

public class EtablissementPrincipalExtractor implements Function<Organisation, EtablissementPrincipal> {

	@Override
	public EtablissementPrincipal apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_PRINCIPAL == ol.getKindOfLocation())
				.findAny()
				.map(EtablissementPrincipal::new)
				.orElse(null);
	}

}