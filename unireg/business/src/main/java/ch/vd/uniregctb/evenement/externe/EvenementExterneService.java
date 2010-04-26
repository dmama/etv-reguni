package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.RegDate;

public interface EvenementExterneService extends EvenementExterneHandler {

	/**
	 * Envoi un événement externe dans la queue de sortie JMS.
	 *
	 * @param businessId l'id de l'événement
	 * @param document   l'événement externe  @throws Exception en cas de probléme
	 * @throws Exception en cas d'erreur
	 */
	public void sendEvent(String businessId, EvenementImpotSourceQuittanceDocument document) throws Exception;

	public EvenementImpotSourceQuittanceDocument createEvenementQuittancement(TypeQuittance.Enum quitancement, Long numeroCtb, RegDate dateDebut,
	                                                                          RegDate dateFin, RegDate dateQuittance);
}
