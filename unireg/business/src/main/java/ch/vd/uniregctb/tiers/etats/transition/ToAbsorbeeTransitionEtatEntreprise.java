package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Transition vers l'état "Absorbée"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public final class ToAbsorbeeTransitionEtatEntreprise extends BaseTransitionEtatEntreprise {

	public ToAbsorbeeTransitionEtatEntreprise(TiersDAO tiersDAO, Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		super(tiersDAO, entreprise, date, generation);
	}

	@Override
	public TypeEtatEntreprise getTypeDestination() {
		return TypeEtatEntreprise.ABSORBEE;
	}

}
