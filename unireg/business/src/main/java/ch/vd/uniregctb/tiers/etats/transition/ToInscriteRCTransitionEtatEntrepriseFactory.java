package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition des états "En faillite", "En liquidation", "Radiée RC" et "Fondée" à l'état "Inscrite RC"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToInscriteRCTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToInscriteRCTransitionEtatEntrepriseFactory(TiersDAO tiersDAO) {
		super(tiersDAO);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (!checkDateValid(actuel, date)) {
			return null;
		}
		switch (actuel.getType()) {
		case FONDEE:
		case EN_FAILLITE:
		case EN_LIQUIDATION:
		case RADIEE_RC:
			return new ToInscriteRCTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
		default:
			return null;
		}
	}
}
