package ch.vd.unireg.xml.party.v3;

import ch.vd.unireg.xml.party.taxresidence.v2.ManagingTaxResidence;
import ch.vd.unireg.xml.DataHelper;

public class ManagingTaxResidenceBuilder {

	public static ManagingTaxResidence newManagingTaxResidence(ch.vd.unireg.tiers.ForGestion forGestion) {
		final ManagingTaxResidence f = new ManagingTaxResidence();
		f.setDateFrom(DataHelper.coreToXMLv2(forGestion.getDateDebut()));
		f.setDateTo(DataHelper.coreToXMLv2(forGestion.getDateFin()));
		f.setMunicipalityFSOId(forGestion.getNoOfsCommune());
		return f;
	}
}
