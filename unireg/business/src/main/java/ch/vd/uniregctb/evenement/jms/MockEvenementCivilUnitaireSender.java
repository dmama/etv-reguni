package ch.vd.uniregctb.evenement.jms;

import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementCivilUnitaireSender implements EvenementCivilUnitaireSender {
	public void sendEvent(EvenementCivilUnitaire evenement, String businessUser) throws Exception {
		// nothing
	}
}
