package ch.vd.uniregctb.evenement.externe;

import org.springframework.transaction.annotation.Transactional;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.QuittanceType;
import ch.vd.registre.base.date.RegDate;

public interface EvenementExterneService extends EvenementExterneHandler {

	/**
	 * Envoi un événement externe dans la queue de sortie JMS.
	 *
	 * @param businessId l'id de l'événement
	 * @param document   l'événement externe  @throws Exception en cas de probléme
	 * @throws Exception en cas d'erreur
	 */
	public void sendEvent(String businessId, EvtQuittanceListeDocument document) throws Exception;

	public EvtQuittanceListeDocument createEvenementQuittancement(QuittanceType.Enum quitancement, Long numeroCtb, RegDate dateDebut,
	                                                                          RegDate dateFin, RegDate dateQuittance);

	/**Permet de traiter les evenements externes depuis un batch de relance
	 *
 	 * @param event
	 * @return true si levenement est traité false si l'evenement n'a pas besoin d'être traité
	 * @throws EvenementExterneException 
	 */
	public boolean traiterEvenementExterne(EvenementExterne event) throws EvenementExterneException;
}
