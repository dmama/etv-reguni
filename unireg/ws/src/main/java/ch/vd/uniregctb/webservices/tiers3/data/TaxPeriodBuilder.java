package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.TaxPeriod;

public class TaxPeriodBuilder {
	public static TaxPeriod newTaxPeriod(ch.vd.uniregctb.declaration.PeriodeFiscale periode) {
		final TaxPeriod p = new TaxPeriod();
		p.setYear(periode.getAnnee());
		return p;
	}
}
