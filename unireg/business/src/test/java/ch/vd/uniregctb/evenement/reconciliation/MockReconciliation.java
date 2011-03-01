package ch.vd.uniregctb.evenement.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockReconciliation extends MockEvenementCivil implements Reconciliation {

	public MockReconciliation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.RECONCILIATION, date, numeroOfsCommuneAnnonce);
	}

	public RegDate getDateReconciliation() {
		return getDate();
	}
}
