package ch.vd.unireg.xml.party.v3;

import ch.vd.unireg.xml.party.taxresidence.v2.TaxationPeriod;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.xml.DataHelper;

public class TaxationPeriodBuilder {

	public static TaxationPeriod newTaxationPeriod(PeriodeImposition periode) {
		final TaxationPeriod p = new TaxationPeriod();
		p.setDateFrom(DataHelper.coreToXMLv2(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv2(periode.getDateFin()));
		p.setTaxDeclarationId(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
