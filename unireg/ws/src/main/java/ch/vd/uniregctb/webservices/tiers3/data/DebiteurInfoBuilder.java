package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;

public class DebiteurInfoBuilder {

	public static DebiteurInfo newDebiteurInfo(GetDebiteurInfoRequest params, List<? extends DateRange> lrEmises, List<DateRange> lrManquantes) {
		DebiteurInfo info = new DebiteurInfo();
		info.setNumeroDebiteur(params.getNumeroDebiteur());
		info.setPeriodeFiscale(params.getPeriodeFiscale());
		info.setNbLRsEmises(lrEmises.size());
		info.setNbLRsTheorique(info.getNbLRsEmises() + count(lrManquantes, params.getPeriodeFiscale()));
		return info;
	}

	/**
	 * Détermine le nombre de LRs existant dans la période fiscale spécifiée
	 *
	 * @param lrs     une liste de LRs
	 * @param periode une période fiscale
	 * @return le nombre de LRs trouvées
	 */
	private static int count(List<? extends DateRange> lrs, int periode) {
		if (lrs == null || lrs.isEmpty()) {
			return 0;
		}
		int c = 0;
		for (DateRange lr : lrs) {
			if (lr.getDateDebut().year() == periode && lr.getDateFin().year() == periode) {
				c++;
			}
		}
		return c;
	}
}
