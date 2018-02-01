package ch.vd.unireg.xml.party.v1;

import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxPeriod;

public class TaxPeriodBuilder {
	public static TaxPeriod newTaxPeriod(ch.vd.unireg.declaration.PeriodeFiscale periode) {
		final TaxPeriod p = new TaxPeriod();
		p.setYear(periode.getAnnee());
		return p;
	}
}
