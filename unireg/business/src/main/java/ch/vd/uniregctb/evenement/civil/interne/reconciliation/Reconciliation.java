package ch.vd.uniregctb.evenement.civil.interne.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

public interface Reconciliation extends EvenementCivilInterne {
	
	public RegDate getDateReconciliation();
	
}
