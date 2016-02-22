package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition des états "Inscrite RC" et "En liquidation" à l'état "En faillite"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToEnFailliteTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToEnFailliteTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, ServiceOrganisationService serviceOrganisation) {
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
		case INSCRITE_RC:
		case FONDEE:
		case EN_LIQUIDATION:
			return new ToEnFailliteTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
		default:
			return null;
		}
	}
}
