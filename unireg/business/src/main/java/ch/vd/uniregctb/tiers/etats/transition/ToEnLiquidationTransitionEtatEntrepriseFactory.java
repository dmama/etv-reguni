package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition de l'état "Inscrite RC" à l'état "En liquidation"
 *
 * @author Raphaël Marmier, 2016-01-22, <raphael.marmier@vd.ch>
 */
public class ToEnLiquidationTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToEnLiquidationTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, EvenementFiscalService evenementFiscalService) {
		super(tiersDAO, evenementFiscalService);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (actuel == null || !checkDateValid(actuel, date)) {
			return null;
		}
		switch (actuel.getType()) {
		case INSCRITE_RC:
			return new ToEnLiquidationTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation, getEvenementFiscalService());
		default:
			return null;
		}
	}
}
