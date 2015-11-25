package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Classe de base dédiée à l'implémentation des méthodes ayant un effet sur Unireg.
 *
 * C'est cette classe qu'il faut étendre pour tout traitement d'événement menant à des opérations sur Unireg. Ces derniers devant être effectués ici.
 *
 */
public abstract class EvenementOrganisationInterneAvecImpactUnireg extends EvenementOrganisationInterne {

	protected EvenementOrganisationInterneAvecImpactUnireg(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                                                       EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, TypeImpact.AVEC_IMPACT_UNIREG);
	}
}
