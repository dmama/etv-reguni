package ch.vd.unireg.evenement.organisation.engine.translator;


import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

public interface EvenementEntrepriseTranslationStrategy {

	/**
	 * Crée un événement entreprise civile interne à partir d'un événement entreprise civile externe.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 * @return L'événement interne qui correspond à l'événement externe reçu, ou null si pas applicable
	 * @throws EvenementEntrepriseException en cas de problème
	 */
	EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event,
	                                          EntrepriseCivile entrepriseCivile,
	                                          Entreprise entreprise) throws EvenementEntrepriseException;
}
