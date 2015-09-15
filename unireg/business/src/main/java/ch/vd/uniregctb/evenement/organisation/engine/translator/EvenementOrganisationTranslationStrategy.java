package ch.vd.uniregctb.evenement.organisation.engine.translator;


import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

public interface EvenementOrganisationTranslationStrategy {

	/**
	 * Crée un événement organisation interne à partir d'un événement organisation externe.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return L'événement interne qui correspond à l'événement externe reçu, ou null si pas applicable
	 * @throws EvenementOrganisationException en cas de problème
	 */
	EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options)
			throws EvenementOrganisationException;
}
