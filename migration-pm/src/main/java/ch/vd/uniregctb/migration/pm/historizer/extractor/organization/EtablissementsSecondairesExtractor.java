package ch.vd.uniregctb.migration.pm.historizer.extractor.organization;

import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.Organisation;

public class EtablissementsSecondairesExtractor implements Function<Organisation, Stream<? extends EtablissementSecondaire>> {

	@Override
	public Stream<EtablissementSecondaire> apply(Organisation org) {
		return org.getOrganisationLocation().stream()
				.filter(ol -> KindOfLocation.ETABLISSEMENT_SECONDAIRE == ol.getKindOfLocation())
				.map(EtablissementSecondaire::new);
	}
 }