package ch.vd.unireg.evenement.organisation.interne.demenagement;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-10-14
 */
public class DemenagementHC extends DemenagementSansChangementDeTypeAutoriteFiscale {
	public DemenagementHC(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                      Entreprise entreprise, EvenementEntrepriseContext context,
	                      EvenementEntrepriseOptions options,
	                      Domicile siegeAvant,
	                      Domicile siegeApres) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public String describe() {
		return "Déménagement hors canton";
	}
}
