package ch.vd.unireg.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

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
