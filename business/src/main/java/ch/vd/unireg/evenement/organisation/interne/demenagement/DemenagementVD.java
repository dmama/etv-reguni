package ch.vd.unireg.evenement.organisation.interne.demenagement;

import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-10-14
 */
public class DemenagementVD extends DemenagementSansChangementDeTypeAutoriteFiscale {
	public DemenagementVD(EvenementOrganisation evenement, Organisation organisation,
	                      Entreprise entreprise, EvenementOrganisationContext context,
	                      EvenementOrganisationOptions options,
	                      Domicile siegeAvant,
	                      Domicile siegeApres) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public String describe() {
		return "Déménagement VD";
	}
}
