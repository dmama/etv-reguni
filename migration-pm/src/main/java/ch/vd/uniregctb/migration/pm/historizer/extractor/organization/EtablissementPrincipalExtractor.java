package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.migration.pm.historizer.extractor.Extractor;

public class EtablissementPrincipalExtractor implements Extractor<Organisation, EtablissementPrincipal> {

	@Nullable
	@Override
	public EtablissementPrincipal apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_PRINCIPAL == ol.getKindOfLocation())
				.findAny()
				.map(EtablissementPrincipal::new)
				.orElse(null);
	}

}