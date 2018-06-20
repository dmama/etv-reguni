package ch.vd.unireg.evenement.entreprise.interne.demenagement;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-10-14
 */
public class DemenagementArrivee extends DemenagementArriveeDepartVD {
	public DemenagementArrivee(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                           Entreprise entreprise, EvenementEntrepriseContext context,
	                           EvenementEntrepriseOptions options,
	                           Domicile siegeAvant,
	                           Domicile siegeApres) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, siegeAvant, siegeApres);
	}

	@Override
	public String describe() {
		return "Déménagement arrivée VD";
	}
}
