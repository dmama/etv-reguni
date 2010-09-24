package ch.vd.uniregctb.evenement.fiscal;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.evenement.EvenementFiscal;

public class MockEvenementFiscalSender implements EvenementFiscalSender {

	public int count;

	private final List<String> trace = new ArrayList<String>();

	public MockEvenementFiscalSender() {
		this.count = 0;
	}

	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		count++;
		trace.add(evenement.toString());
	}

	/**
	 * Permet, en debug, de visualiser les événements qui sont passés
	 */
	public List<String> getTrace() {
		return trace;
	}
}