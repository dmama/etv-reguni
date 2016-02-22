package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition de l'état "Inscrite RC" à l'état "Fondée"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToFondeeTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToFondeeTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, ServiceOrganisationService serviceOrganisation) {
		super(tiersDAO, serviceOrganisation);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (actuel == null) {
			if (!isInscriteRC(entreprise, date)) {
				return new ToFondeeTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
			} else {
				return null;
			}
		}
		if (!checkDateValid(actuel, date)) {
			return null;
		}
		switch (actuel.getType()) {
		case INSCRITE_RC:
			return new ToFondeeTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
		case EN_FAILLITE:
			if (!isInscriteRC(entreprise, date)) {
				return new ToFondeeTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
			}
			return null;
		default:
			return null;
		}
	}
}
