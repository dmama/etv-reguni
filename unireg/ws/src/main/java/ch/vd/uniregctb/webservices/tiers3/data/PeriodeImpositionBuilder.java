package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.PeriodeImposition;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

public class PeriodeImpositionBuilder {
	public static PeriodeImposition newPeriodeImposition(ch.vd.uniregctb.metier.assujettissement.PeriodeImposition periode) {
		final PeriodeImposition p = new PeriodeImposition();
		p.setDateDebut(DataHelper.coreToWeb(periode.getDateDebut()));
		p.setDateFin(DataHelper.coreToWeb(periode.getDateFin()));
		p.setIdDI(DataHelper.getAssociatedDi(periode));
		return p;
	}
}
