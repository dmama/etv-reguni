package ch.vd.unireg.xml.party.v4;

import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxPeriod;

public class TaxPeriodBuilder {
	public static TaxPeriod newTaxPeriod(ch.vd.unireg.declaration.PeriodeFiscale periode) {
		final TaxPeriod p = new TaxPeriod();
		p.setYear(periode.getAnnee());
		return p;
	}
}
