package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;

/**
 * Interface qui permet d'envoyer des événements externes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementExterneSender {
	void sendEvent(String businessId, EvenementImpotSourceQuittanceDocument document) throws Exception;
}
