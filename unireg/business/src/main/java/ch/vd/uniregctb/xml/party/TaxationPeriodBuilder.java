package ch.vd.uniregctb.xml.party;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxationPeriod;
import ch.vd.uniregctb.xml.DataHelper;

public class TaxationPeriodBuilder {
	public static TaxationPeriod newTaxationPeriod(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periode) {
		final TaxationPeriod p = new TaxationPeriod();
		p.setDateFrom(DataHelper.coreToXML(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToXML(periode.getDateFin()));
		p.setTaxDeclarationId(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
