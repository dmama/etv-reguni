package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.tiers.Entreprise;

/**
 * <p>
 *      Classe à étendre pour le traitement d'événement en utilisant les services Unireg sauf celui servant à l'envoi d'événements
 *      fiscaux. (Les services d'Unireg appelés dans le cadre du traitement émetteront eux-même, le cas échéant, des événements fiscaux)
 * </p>
 * <p>
 *      La distinction existe pour pouvoir relancer les événements fiscaux d'information qui sinon seraient
 *      perdus lorsque l'utilisateur force un événement RCEnt.
 * </p>
 */
public abstract class EvenementOrganisationInterneDeTraitement extends EvenementOrganisationInterne {

	protected EvenementOrganisationInterneDeTraitement(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final EvenementOrganisationInterne seulementEvenementsFiscaux() {
		// On ne renvoie rien car on n'est pas générateur direct d'événements fiscaux seulement.
		return null;
	}
}
