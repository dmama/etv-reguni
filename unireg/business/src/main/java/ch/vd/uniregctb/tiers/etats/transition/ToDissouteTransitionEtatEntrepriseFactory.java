package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition de l'état "Fondée" à l'état "Dissoute"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToDissouteTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToDissouteTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, ServiceOrganisationService serviceOrganisation) {
		super(tiersDAO, serviceOrganisation);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (actuel == null) {
			return null;
		}
		if (!checkDateValid(actuel, date)) {
			return null;
		}
		switch (actuel.getType()) {
		case FONDEE:
			return new ToDissouteTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
		case EN_FAILLITE:
			if (!isInscriteRC(entreprise, date)) {
				return new ToDissouteTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
			}
			return null;
		default:
			return null;
	}
	}
}
