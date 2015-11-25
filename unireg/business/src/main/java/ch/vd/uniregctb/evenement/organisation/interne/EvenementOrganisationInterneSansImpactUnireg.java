package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Classe de dédiée à l'implémentation des méthodes N'ayant PAS d'effet sur Unireg.
 *
 * Note importante: - Les classe dérivée s'engagent à n'avoir aucun impact Unireg. Autrement, il faut étendre
 *                    EvenementOrganisationInterneAvecImpactUnireg. La distinction existe pour pouvoir lancer les
 *                    événements fiscaux qui sinon ne le seraient pas lorsque l'utilisateur passe un événement RCEnt
 *                    de EN_ERREUR à FORCE.
 */
public abstract class EvenementOrganisationInterneSansImpactUnireg extends EvenementOrganisationInterne {

	protected EvenementOrganisationInterneSansImpactUnireg(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                                                       EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, TypeImpact.SANS_IMPACT_UNIREG);
	}
}
