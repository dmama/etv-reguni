package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.uniregctb.evenement.EvenementFiscal;

public class MockEvenementFiscalSender implements EvenementFiscalSender {

	public  int count;

	public MockEvenementFiscalSender() {
		this.count = 0;
	}

	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		count++;
	}
}