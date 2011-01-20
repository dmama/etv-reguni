package ch.vd.uniregctb.evenement.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class ReconciliationAdapter extends GenericEvenementAdapter implements Reconciliation {

	public RegDate getDateReconciliation() {
		return getDate();
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
