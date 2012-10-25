package ch.vd.uniregctb.evenement.civil.regpp.jms;

import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementCivilSender implements EvenementCivilSender {
	
	@Override
	public void sendEvent(EvenementCivilRegPP evenement, String businessUser) throws Exception {
		// nothing
	}
}
