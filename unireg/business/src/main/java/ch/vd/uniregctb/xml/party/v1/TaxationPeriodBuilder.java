package ch.vd.uniregctb.xml.party.v1;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxationPeriod;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.xml.DataHelper;

public class TaxationPeriodBuilder {

	public static TaxationPeriod newTaxationPeriod(PeriodeImposition periode) {
		final TaxationPeriod p = new TaxationPeriod();
		p.setDateFrom(DataHelper.coreToXMLv1(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv1(periode.getDateFin()));
		p.setTaxDeclarationId(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
