package ch.vd.unireg.evenement.organisation.engine.translator;


import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;

/**
 * Cette interface expose les méthodes qui permette de traduire des événements entreprise externes en un événement entreprise interne.
 */
public interface EvenementEntrepriseTranslator {

	/**
	 * Traduit un événement entreprise externe (qui nous vient du registre entreprise RCEnt) en un événement interne ou un événement
	 * interne composite regroupant une liste d'événements internes représentant chacun un événement métier distinct.
	 *
	 * @param event   un événement entreprise externe
	 * @return l'événement entreprise interne correspondant
	 * @throws EvenementEntrepriseException en cas de problème
	 */
	EvenementEntrepriseInterne toInterne(EvenementEntreprise event) throws EvenementEntrepriseException;
}
