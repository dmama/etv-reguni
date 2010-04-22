package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;

/**
 * Bean qui ne fait rien.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementExterneSender implements EvenementExterneSender {

	public void sendEvent(String businessId, EvenementImpotSourceQuittanceDocument document) throws Exception {
	}
}