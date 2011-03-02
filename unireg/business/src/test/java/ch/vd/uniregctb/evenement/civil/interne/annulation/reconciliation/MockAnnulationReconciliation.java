package ch.vd.uniregctb.evenement.civil.interne.annulation.reconciliation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationReconciliation extends MockEvenementCivil implements AnnulationReconciliation {
	public MockAnnulationReconciliation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_RECONCILIATION, date, numeroOfsCommuneAnnonce);
	}
}
