package ch.vd.uniregctb.evenement.docsortant;

import ch.vd.unireg.xml.event.docsortant.v1.Documents;

public class MockEvenementDocumentSortantSender implements EvenementDocumentSortantSender {

	@Override
	public void sendEvenementDocumentSortant(String businessId, Documents docs) throws EvenementDocumentSortantException {
		// mock = ne fait rien...
	}
}
