package ch.vd.uniregctb.evenement.jms;

import ch.vd.uniregctb.evenement.EvenementCivilData;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementCivilSender implements EvenementCivilSender {
	
	public void sendEvent(EvenementCivilData evenement, String businessUser) throws Exception {
		// nothing
	}
}
