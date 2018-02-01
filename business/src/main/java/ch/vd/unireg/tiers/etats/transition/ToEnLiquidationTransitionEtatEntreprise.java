package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Transition vers l'état "En liquidation"
 *
 * @author Raphaël Marmier, 2016-01-21, <raphael.marmier@vd.ch>
 */
public final class ToEnLiquidationTransitionEtatEntreprise extends BaseTransitionEtatEntreprise {

	public ToEnLiquidationTransitionEtatEntreprise(TiersDAO tiersDAO, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation, EvenementFiscalService evenementFiscalService) {
		super(tiersDAO, entreprise, date, generation, evenementFiscalService);
	}

	@Override
	public EtatEntreprise apply() {
		EtatEntreprise etat = super.apply();
		getEvenementFiscalService().publierEvenementFiscalInformationComplementaire(getEntreprise(),
		                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.LIQUIDATION,
		                                                                       getDate());

		return etat;
	}

	@Override
	public TypeEtatEntreprise getTypeDestination() {
		return TypeEtatEntreprise.EN_LIQUIDATION;
	}
}
