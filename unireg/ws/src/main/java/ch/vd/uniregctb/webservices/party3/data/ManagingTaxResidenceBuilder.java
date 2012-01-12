package ch.vd.uniregctb.webservices.party3.data;

import ch.vd.unireg.xml.party.taxresidence.v1.ManagingTaxResidence;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;

public class ManagingTaxResidenceBuilder {
	public static ManagingTaxResidence newManagingTaxResidence(ch.vd.uniregctb.tiers.ForGestion forGestion) {
		final ManagingTaxResidence f = new ManagingTaxResidence();
		f.setDateFrom(DataHelper.coreToWeb(forGestion.getDateDebut()));
		f.setDateTo(DataHelper.coreToWeb(forGestion.getDateFin()));
		f.setMunicipalityFSOId(forGestion.getNoOfsCommune());
		return f;
	}
}
