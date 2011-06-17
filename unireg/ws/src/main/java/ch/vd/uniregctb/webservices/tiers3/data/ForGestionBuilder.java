package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.ForGestion;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

public class ForGestionBuilder {
	public static ForGestion newForGestion(ch.vd.uniregctb.tiers.ForGestion forGestion) {
		final ForGestion f = new ForGestion();
		f.setDateDebut(DataHelper.coreToWeb(forGestion.getDateDebut()));
		f.setDateFin(DataHelper.coreToWeb(forGestion.getDateFin()));
		f.setNoOfsCommune(forGestion.getNoOfsCommune());
		return f;
	}
}
