package ch.vd.uniregctb.evenement.fiscal;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CollectingEvenementFiscalSender implements EvenementFiscalSender {

	private final AtomicInteger count;
	private final List<String> trace = new LinkedList<>();

	public CollectingEvenementFiscalSender() {
		this.count = new AtomicInteger(0);
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {
		count.incrementAndGet();
		trace.add(evenement.toString());
	}

	public void reset() {
		count.set(0);
	}

	public int getCount() {
		return count.intValue();
	}

	/**
	 * Permet, en debug, de visualiser les événements qui sont passés
	 */
	public List<String> getTrace() {
		return trace;
	}
}
