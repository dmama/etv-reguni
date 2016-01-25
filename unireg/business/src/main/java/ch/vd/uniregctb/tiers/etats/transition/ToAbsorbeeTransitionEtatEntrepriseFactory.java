package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition des états "Inscrite RC" et "Fondée" à l'état "Absorbée"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToAbsorbeeTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToAbsorbeeTransitionEtatEntrepriseFactory(TiersDAO tiersDAO) {
		super(tiersDAO);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (checkDateValid(actuel, date)) {
			switch (actuel.getType()) {
			case INSCRITE_RC:
			case FONDEE:
				return new ToAbsorbeeTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
			}
		}
		return null;
	}
}
