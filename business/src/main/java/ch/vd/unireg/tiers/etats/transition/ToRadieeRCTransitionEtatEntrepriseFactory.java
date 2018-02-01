package ch.vd.unireg.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition des états "En faillite", "En liquidation" et "Absorbée" à l'état "Radiée RC"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToRadieeRCTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToRadieeRCTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, EvenementFiscalService evenementFiscalService) {
		super(tiersDAO, evenementFiscalService);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (actuel == null || !checkDateValid(actuel, date)) {
			return null;
		}
		switch (actuel.getType()) {
		case EN_LIQUIDATION:
		case EN_FAILLITE:
		case ABSORBEE:
		case INSCRITE_RC: // Nécessaire pour les cas de radiation APM sans dissolution
			return new ToRadieeRCTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation, getEvenementFiscalService());
		default:
			return null;
		}
	}

}
