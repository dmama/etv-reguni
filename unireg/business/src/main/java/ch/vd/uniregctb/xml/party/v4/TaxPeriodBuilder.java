package ch.vd.uniregctb.xml.party.v4;

import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxPeriod;

public class TaxPeriodBuilder {
	public static TaxPeriod newTaxPeriod(ch.vd.uniregctb.declaration.PeriodeFiscale periode) {
		final TaxPeriod p = new TaxPeriod();
		p.setYear(periode.getAnnee());
		return p;
	}
}
