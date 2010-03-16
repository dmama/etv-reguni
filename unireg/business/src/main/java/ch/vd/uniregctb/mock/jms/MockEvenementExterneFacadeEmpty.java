package ch.vd.uniregctb.mock.jms;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.uniregctb.evenement.externe.DelegateEvenementExterne;
import ch.vd.uniregctb.evenement.externe.EvenementExterneFacade;
import ch.vd.uniregctb.evenement.externe.IEvenementExterne;

public class MockEvenementExterneFacadeEmpty implements EvenementExterneFacade {

	public EvenementImpotSourceQuittanceType creerEvenementImpotSource() {
		return null;
	}

	public void sendEvent(IEvenementExterne evenementExterne) throws Exception {
	}

	public void setDelegate(DelegateEvenementExterne delegate) {
	}

}
