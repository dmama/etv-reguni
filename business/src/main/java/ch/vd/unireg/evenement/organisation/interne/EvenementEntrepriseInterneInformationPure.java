package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

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
public abstract class EvenementEntrepriseInterneInformationPure extends EvenementEntrepriseInterne {

	protected EvenementEntrepriseInterneInformationPure(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise, EvenementEntrepriseContext context,
	                                                    EvenementEntrepriseOptions options) {
		super(evenement, entrepriseCivile, entreprise, context, options);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final EvenementEntrepriseInterne seulementEvenementsFiscaux() {
		// On est générateur direct d'événements fiscaux seulement.
		return this;
	}
}
