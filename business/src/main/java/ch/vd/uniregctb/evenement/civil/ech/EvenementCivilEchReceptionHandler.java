package ch.vd.uniregctb.evenement.civil.ech;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;

/**
 * Interface du service de traitement à la réception d'un événement civil. Ce composant à été découplé pour faciliter les tests.
 */
public interface EvenementCivilEchReceptionHandler {

	/**
	 * Appelé une fois le contenu XML décodé afin de persister le nouvel événement en base de données
	 * @param event événement à persister
	 * @return <code>null</code> si l'événement n'a pas été persisté (existe déjà), sinon événement modifié par la persistence
	 */
	EvenementCivilEch saveIncomingEvent(EvenementCivilEch event);

	/**
	 * Appelé une fois que l'événement civil est sauvegardé en base. En particulier, le numéro d'individu civil n'est pas encore connu.
	 * @param event événement reçu
	 * @param mode suivant la nature de l'appelant (un traitement batch ou un utilisateur humain), ou <b><code>null</code></b> s'il ne faut pas lancer le traitement mais juste se cantonner à l'attribution du numéro d'individu
	 * @return événement potentiellement modifié après prise en compte (avec numéro d'individu, par exemple...)
	 * @throws EvenementCivilException en cas de problème
	 */
	EvenementCivilEch handleEvent(EvenementCivilEch event, @Nullable EvenementCivilEchProcessingMode mode) throws EvenementCivilException;

}
