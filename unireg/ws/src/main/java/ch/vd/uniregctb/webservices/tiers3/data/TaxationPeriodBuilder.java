package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.TaxationPeriod;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

public class TaxationPeriodBuilder {
	public static TaxationPeriod newTaxationPeriod(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periode) {
		final TaxationPeriod p = new TaxationPeriod();
		p.setDateFrom(DataHelper.coreToWeb(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToWeb(periode.getDateFin()));
		p.setTaxDeclarationId(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
