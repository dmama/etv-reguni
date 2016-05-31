package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-10-14
 */
public class DemenagementArrivee extends DemenagementArriveeDepartVD {
	public DemenagementArrivee(EvenementOrganisation evenement, Organisation organisation,
	                           Entreprise entreprise, EvenementOrganisationContext context,
	                           EvenementOrganisationOptions options,
	                           Domicile siegeAvant,
	                           Domicile siegeApres) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public String describe() {
		return "Déménagement arrivée VD";
	}
}
