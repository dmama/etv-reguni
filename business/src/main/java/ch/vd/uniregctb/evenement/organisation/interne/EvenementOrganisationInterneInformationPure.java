package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * <p>
 *      Classe à étendre pour le traitement d'événement sans appel à des services Unireg autre que le service d'émission d'événement fiscaux.
 *      Les classe dérivée s'engagent à avoir pour seule action l'envoi d'événements fiscaux Unireg.
 * </p>
 * <p>
 *      La distinction existe pour pouvoir relancer les événements fiscaux d'information qui sinon seraient
 *      perdus lorsque l'utilisateur force un événement RCEnt.
 * </p>
 */
public abstract class EvenementOrganisationInterneInformationPure extends EvenementOrganisationInterne {

	protected EvenementOrganisationInterneInformationPure(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                                                      EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final EvenementOrganisationInterne seulementEvenementsFiscaux() {
		// On est générateur direct d'événements fiscaux seulement.
		return this;
	}
}
