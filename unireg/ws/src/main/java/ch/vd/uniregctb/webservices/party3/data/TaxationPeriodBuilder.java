package ch.vd.uniregctb.webservices.party3.data;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxationPeriod;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;

public class TaxationPeriodBuilder {
	public static TaxationPeriod newTaxationPeriod(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periode) {
		final TaxationPeriod p = new TaxationPeriod();
		p.setDateFrom(DataHelper.coreToWeb(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToWeb(periode.getDateFin()));
		p.setTaxDeclarationId(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
