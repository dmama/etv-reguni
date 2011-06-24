package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.DebtorPeriodicity;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class DebtorPeriodicityBuilder {
	public static DebtorPeriodicity newPeriodicity(ch.vd.uniregctb.declaration.Periodicite periodicite) {
		final DebtorPeriodicity p = new DebtorPeriodicity();
		p.setDateFrom(DataHelper.coreToWeb(periodicite.getDateDebut()));
		p.setDateTo(DataHelper.coreToWeb(periodicite.getDateFin()));
		p.setCancellationDate(DataHelper.coreToWeb(periodicite.getAnnulationDate()));
		p.setPeriodicity(EnumHelper.coreToWeb(periodicite.getPeriodiciteDecompte()));
		p.setSpecificPeriod(EnumHelper.coreToWeb(periodicite.getPeriodeDecompte()));
		return p;
	}

}
