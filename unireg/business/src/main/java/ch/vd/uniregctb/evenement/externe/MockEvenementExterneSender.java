package ch.vd.uniregctb.evenement.externe;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;

/**
 * Bean qui ne fait rien.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockEvenementExterneSender implements EvenementExterneSender {

	public void sendEvent(String businessId, EvtQuittanceListeDocument document) throws Exception {
	}
}