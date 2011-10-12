package ch.vd.uniregctb.webservices.party3.data;

import ch.vd.unireg.xml.party.debtor.v1.DebtorPeriodicity;
import ch.vd.uniregctb.webservices.party3.impl.DataHelper;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

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
