package ch.vd.uniregctb.evenement.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

public class MockReconciliation extends MockEvenementCivil implements Reconciliation {

	public RegDate getDateReconciliation() {
		return getDate();
	}
}
