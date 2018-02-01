package ch.vd.unireg.evenement.organisation;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Interface du service de traitement à la réception d'un événement organisation. Ce composant à été découplé pour faciliter les tests.
 */
public interface EvenementOrganisationReceptionHandler {

	/**
	 * Contrôle si un événement RCEnt a déjà été receptionné pour le businessId.
	 *
	 * @param businessId le businessId de l'événement à contrôler
	 * @return <code>true</code> si l'événement correspondant au businessId a déjà été reçu. <code>false</code> sinon.
	 */
	boolean dejaRecu(String businessId);

	/**
	 * Appelé une fois le contenu XML décodé afin de persister les nouveaux événements (dérivés de l'événement en arrivée) en base de données
	 * @param events événements à persister
	 * @return <code>null</code> si les événements n'ont pas été persistés (événement organisation déjà reçu), sinon les événements persistés
	 */
	@NotNull
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
