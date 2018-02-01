package ch.vd.unireg.xml.party.v2;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxationPeriod;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.xml.DataHelper;

public class TaxationPeriodBuilder {

	public static TaxationPeriod newTaxationPeriod(PeriodeImposition periode) {
		final TaxationPeriod p = new TaxationPeriod();
		p.setDateFrom(DataHelper.coreToXMLv1(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv1(periode.getDateFin()));
		p.setTaxDeclarationId(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
