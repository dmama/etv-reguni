package ch.vd.unireg.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * Transition vers l'état "En faillite"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public final class ToEnFailliteTransitionEtatEntreprise extends BaseTransitionEtatEntreprise {

	public ToEnFailliteTransitionEtatEntreprise(TiersDAO tiersDAO, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation, EvenementFiscalService evenementFiscalService) {
		super(tiersDAO, entreprise, date, generation, evenementFiscalService);
	}

	@Override
	public TypeEtatEntreprise getTypeDestination() {
		return TypeEtatEntreprise.EN_FAILLITE;
	}
}
