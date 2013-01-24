package ch.vd.uniregctb.xml.party;

import ch.vd.unireg.xml.party.taxresidence.v1.ManagingTaxResidence;
import ch.vd.uniregctb.xml.DataHelper;

public class ManagingTaxResidenceBuilder {
	public static ManagingTaxResidence newManagingTaxResidence(ch.vd.uniregctb.tiers.ForGestion forGestion) {
		final ManagingTaxResidence f = new ManagingTaxResidence();
		f.setDateFrom(DataHelper.coreToXML(forGestion.getDateDebut()));
		f.setDateTo(DataHelper.coreToXML(forGestion.getDateFin()));
		f.setMunicipalityFSOId(forGestion.getNoOfsCommune());
		return f;
	}
}
