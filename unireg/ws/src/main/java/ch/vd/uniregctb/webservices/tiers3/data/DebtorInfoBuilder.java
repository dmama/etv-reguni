package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.webservices.tiers3.GetDebtorInfoRequest;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;

public class DebtorInfoBuilder {

	public static DebtorInfo newDebtorInfo(GetDebtorInfoRequest params, List<? extends DateRange> lrEmises, List<DateRange> lrManquantes) {
		DebtorInfo info = new DebtorInfo();
		info.setNumber(params.getDebtorNumber());
		info.setTaxPeriod(params.getTaxPeriod());
		info.setNumberOfWithholdingTaxDeclarationsIssued(lrEmises.size());
		info.setTheoreticalNumberOfWithholdingTaxDeclarations(info.getNumberOfWithholdingTaxDeclarationsIssued() + count(lrManquantes, params.getTaxPeriod()));
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
