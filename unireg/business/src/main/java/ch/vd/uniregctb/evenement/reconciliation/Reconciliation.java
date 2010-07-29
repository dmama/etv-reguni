package ch.vd.uniregctb.evenement.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivil;

public interface Reconciliation extends EvenementCivil {
	
	public RegDate getDateReconciliation();
	
}
