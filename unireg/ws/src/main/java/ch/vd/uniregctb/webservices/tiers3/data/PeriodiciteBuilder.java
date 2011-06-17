package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.Periodicite;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class PeriodiciteBuilder {
	public static Periodicite newPeriodicite(ch.vd.uniregctb.declaration.Periodicite periodicite) {
		final Periodicite p = new Periodicite();
		p.setDateDebut(DataHelper.coreToWeb(periodicite.getDateDebut()));
		p.setDateFin(DataHelper.coreToWeb(periodicite.getDateFin()));
		p.setDateAnnulation(DataHelper.coreToWeb(periodicite.getAnnulationDate()));
		p.setPeriodiciteDecompte(EnumHelper.coreToWeb(periodicite.getPeriodiciteDecompte()));
		p.setPeriodeDecompte(EnumHelper.coreToWeb(periodicite.getPeriodeDecompte()));
		return p;
	}

}
