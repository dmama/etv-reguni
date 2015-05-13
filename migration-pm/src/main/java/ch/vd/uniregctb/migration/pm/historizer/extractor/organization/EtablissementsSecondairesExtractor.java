package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;
import ch.vd.uniregctb.migration.pm.historizer.extractor.Extractor;

public class EtablissementsSecondairesExtractor implements Extractor<Organisation, Stream<? extends EtablissementSecondaire>> {

	@Nullable
	@Override
	public Stream<EtablissementSecondaire> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_SECONDAIRE == ol.getKindOfLocation())
				.map(EtablissementSecondaire::new);
	}

}