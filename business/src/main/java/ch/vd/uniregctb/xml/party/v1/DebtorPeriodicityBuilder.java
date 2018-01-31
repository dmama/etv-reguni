package ch.vd.uniregctb.xml.party.v1;

import ch.vd.unireg.xml.party.debtor.v1.DebtorPeriodicity;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class DebtorPeriodicityBuilder {
	public static DebtorPeriodicity newPeriodicity(ch.vd.uniregctb.declaration.Periodicite periodicite) {
		final DebtorPeriodicity p = new DebtorPeriodicity();
		p.setDateFrom(DataHelper.coreToXMLv1(periodicite.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv1(periodicite.getDateFin()));
		p.setCancellationDate(DataHelper.coreToXMLv1(periodicite.getAnnulationDate()));
		p.setPeriodicity(EnumHelper.coreToXMLv1(periodicite.getPeriodiciteDecompte()));
		p.setSpecificPeriod(EnumHelper.coreToXMLv1(periodicite.getPeriodeDecompte()));
		return p;
	}

}
