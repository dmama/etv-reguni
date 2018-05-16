package ch.vd.unireg.evenement.organisation.engine.translator;


import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;

public interface EvenementOrganisationTranslationStrategy {

	/**
	 * Crée un événement organisation interne à partir d'un événement organisation externe.
	 *
	 * @param event un événement organisation reçu de RCEnt
	 * @return L'événement interne qui correspond à l'événement externe reçu, ou null si pas applicable
	 * @throws EvenementOrganisationException en cas de problème
	 */
	EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                            Organisation organisation,
	                                            Entreprise entreprise) throws EvenementOrganisationException;
}
