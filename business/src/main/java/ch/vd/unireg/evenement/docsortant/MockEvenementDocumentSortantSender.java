package ch.vd.unireg.evenement.docsortant;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.event.docsortant.v1.Documents;

public class MockEvenementDocumentSortantSender implements EvenementDocumentSortantSender {

	@Override
	public void sendEvenementDocumentSortant(String businessId, Documents docs, boolean reponseAttendue, @Nullable Map<String, String> additionalHeaders) throws EvenementDocumentSortantException {
		// mock = ne fait rien...
	}
}
