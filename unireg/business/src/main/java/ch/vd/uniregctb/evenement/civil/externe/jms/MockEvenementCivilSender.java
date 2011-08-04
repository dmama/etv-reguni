package ch.vd.uniregctb.evenement.civil.externe.jms;

import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementCivilSender implements EvenementCivilSender {
	
	@Override
	public void sendEvent(EvenementCivilExterne evenement, String businessUser) throws Exception {
		// nothing
	}
}
