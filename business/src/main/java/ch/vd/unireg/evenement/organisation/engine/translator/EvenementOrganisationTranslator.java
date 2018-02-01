package ch.vd.unireg.evenement.organisation.engine.translator;


import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;

/**
 * Cette interface expose les méthodes qui permette de traduire des événements organisation externes en un événement organisation interne.
 */
public interface EvenementOrganisationTranslator {

	/**
	 * Traduit un événement organisation externe (qui nous vient du registre organisation RCEnt) en un événement interne ou un événement
	 * interne composite regroupant une liste d'événements internes représentant chacun un événement métier distinct.
	 *
	 * @param event   un événement organisation externe
	 * @return l'événement organisation interne correspondant
	 * @throws EvenementOrganisationException en cas de problème
	 */
	EvenementOrganisationInterne toInterne(EvenementOrganisation event) throws EvenementOrganisationException;
}
