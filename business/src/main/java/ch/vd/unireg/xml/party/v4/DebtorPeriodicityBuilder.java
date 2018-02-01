package ch.vd.unireg.xml.party.v4;

import ch.vd.unireg.xml.party.withholding.v1.DebtorPeriodicity;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public class DebtorPeriodicityBuilder {

	public static DebtorPeriodicity newPeriodicity(ch.vd.unireg.declaration.Periodicite periodicite) {
		final DebtorPeriodicity p = new DebtorPeriodicity();
		p.setDateFrom(DataHelper.coreToXMLv2(periodicite.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv2(periodicite.getDateFin()));
		p.setCancellationDate(DataHelper.coreToXMLv2(periodicite.getAnnulationDate()));
		p.setPeriodicity(EnumHelper.coreToXMLv3(periodicite.getPeriodiciteDecompte()));
		p.setSpecificPeriod(EnumHelper.coreToXMLv3(periodicite.getPeriodeDecompte()));
		return p;
	}

}
