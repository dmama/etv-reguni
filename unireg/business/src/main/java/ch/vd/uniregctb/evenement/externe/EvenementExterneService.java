package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;

public interface EvenementExterneService extends EvenementExterneHandler {

	/**
	 * Envoi un événement externe dans la queue de sortie JMS.
	 *
	 * @param businessId l'id de l'événement
	 * @param document   l'événement externe  @throws Exception en cas de probléme
	 * @throws Exception en cas d'erreur
	 */
	void sendEvent(String businessId, EvtQuittanceListeDocument document) throws Exception;

	/**
	 * Permet de re-traiter les evenements externes depuis un batch de relance
	 *
 	 * @param event événement à re-traiter
	 * @return <code>true</code> si l'événement a été traité, <code>false</code> s'il est parti/resté en erreur
	 */
	boolean retraiterEvenementExterne(EvenementExterne event);
}
