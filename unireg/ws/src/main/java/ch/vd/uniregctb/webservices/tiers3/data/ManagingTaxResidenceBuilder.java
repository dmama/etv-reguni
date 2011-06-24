package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.ManagingTaxResidence;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

public class ManagingTaxResidenceBuilder {
	public static ManagingTaxResidence newManagingTaxResidence(ch.vd.uniregctb.tiers.ForGestion forGestion) {
		final ManagingTaxResidence f = new ManagingTaxResidence();
		f.setDateFrom(DataHelper.coreToWeb(forGestion.getDateDebut()));
		f.setDateTo(DataHelper.coreToWeb(forGestion.getDateFin()));
		f.setMunicipalityFSOId(forGestion.getNoOfsCommune());
		return f;
	}
}
