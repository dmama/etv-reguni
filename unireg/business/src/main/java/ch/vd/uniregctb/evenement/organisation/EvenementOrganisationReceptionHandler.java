package ch.vd.uniregctb.evenement.organisation;

/**
 * Interface du service de traitement à la réception d'un événement organisation. Ce composant à été découplé pour faciliter les tests.
 */
public interface EvenementOrganisationReceptionHandler {

	/**
	 * Appelé une fois le contenu XML décodé afin de persister le nouvel événement en base de données
	 * @param event événement à persister
	 * @return <code>null</code> si l'événement n'a pas été persisté (existe déjà), sinon événement modifié par la persistence
	 */
	EvenementOrganisation saveIncomingEvent(EvenementOrganisation event);

	/**
	 * Appelé une fois que l'événement organisation est sauvegardé en base.
	 * @param event événement reçu
	 * @param mode suivant la nature de l'appelant (un traitement batch ou un utilisateur humain)
	 * @return événement potentiellement modifié après prise en compte (avec numéro d'individu, par exemple...)
	 * @throws EvenementOrganisationException en cas de problème
	 */
	EvenementOrganisation handleEvent(EvenementOrganisation event, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException;

}
