package ch.vd.uniregctb.evenement.externe;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;

/**
 * Interface qui permet d'envoyer des événements externes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementExterneSender {
	void sendEvent(String businessId, EvtQuittanceListeDocument document) throws Exception;
}
