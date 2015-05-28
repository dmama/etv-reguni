package ch.vd.uniregctb.migration.pm.historizer;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.OrganisationLocation;

public class EtablissementsBuilder {
	public static List<OrganisationLocation> build(Map<BigInteger, List<DateRanged<ch.vd.evd0022.v1.OrganisationLocation>>> detailEtablissements) {
		// Ca ne joue pas, il faut récupérer les contenu des établissements champ par champ car on le présente comme cela.
		return null;
	}
}
