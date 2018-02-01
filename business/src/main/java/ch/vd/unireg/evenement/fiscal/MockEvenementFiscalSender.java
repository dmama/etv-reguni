package ch.vd.unireg.evenement.fiscal;

public class MockEvenementFiscalSender implements EvenementFiscalSender {

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		// ne fait rien, c'est un mock...
	}

}