package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.uniregctb.webservices.tiers3.PeriodeFiscale;

public class PeriodeFiscaleBuilder {
	public static PeriodeFiscale newPeriodeFiscale(ch.vd.uniregctb.declaration.PeriodeFiscale periode) {
		final PeriodeFiscale p = new PeriodeFiscale();
		p.setAnnee(periode.getAnnee());
		return p;
	}
}
