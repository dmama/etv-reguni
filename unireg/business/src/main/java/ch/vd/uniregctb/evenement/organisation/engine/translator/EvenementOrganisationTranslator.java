package ch.vd.uniregctb.evenement.organisation.engine.translator;


import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;

/**
 * Cette interface expose les méthodes qui permette de traduire des événements organisation externes en un événement organisation interne.
 */
public interface EvenementOrganisationTranslator {

	/**
	 * Traduit un événement organisation externe (qui nous vient du registre organisation RCEnt) en un événement interne ou un événement
	 * interne composite regroupant une liste d'événements internes représentant chacun un événement métier distinct.
	 *
	 * @param event   un événement organisation externe
	 * @param options les options d'exécution de l'événement
	 * @return l'événement organisation interne correspondant
	 * @throws EvenementOrganisationException en cas de problème
	 */
	EvenementOrganisationInterne toInterne(EvenementOrganisation event, EvenementOrganisationOptions options) throws EvenementOrganisationException;

	/**
	 * @param event un événement organisation externe
	 * @return <code>true</code> dans le cas où le seul traitement de cet événement sera une ré-indexation du tiers, <code>false</code> s'il y a plus
	 */
//	boolean isIndexationOnly(EvenementOrganisation event); // Probablement pas applicable à ce niveau mais en aval
}
