package ch.vd.unireg.xml.party.v3;

import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxPeriod;

public class TaxPeriodBuilder {
	public static TaxPeriod newTaxPeriod(ch.vd.unireg.declaration.PeriodeFiscale periode) {
		final TaxPeriod p = new TaxPeriod();
		p.setYear(periode.getAnnee());
		return p;
	}
}
