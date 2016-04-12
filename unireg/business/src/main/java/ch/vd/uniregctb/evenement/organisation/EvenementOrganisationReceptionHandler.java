package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

/**
 * Interface du service de traitement à la réception d'un événement organisation. Ce composant à été découplé pour faciliter les tests.
 */
public interface EvenementOrganisationReceptionHandler {

	/**
	 * Appelé une fois le contenu XML décodé afin de persister les nouveaux événements (dérivés de l'événement en arrivée) en base de données
	 * @param events événements à persister
	 * @return <code>null</code> si les événements n'ont pas été persistés (événement organisation déjà reçu), sinon les événements persistés
	 */
	List<EvenementOrganisation> saveIncomingEvent(List<EvenementOrganisation> events);

	/**
	 * Appelé une fois que les événements organisation sont sauvegardés en base. Programme le traitement de chacun séparément.
	 * @param events événements reçus
	 * @param mode suivant la nature de l'appelant (un traitement batch ou un utilisateur humain)
	 * @return événement potentiellement modifié après prise en compte (avec numéro d'individu, par exemple...)
	 * @throws EvenementOrganisationException en cas de problème
	 */
	List<EvenementOrganisation> handleEvents(List<EvenementOrganisation> events, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException;

}
