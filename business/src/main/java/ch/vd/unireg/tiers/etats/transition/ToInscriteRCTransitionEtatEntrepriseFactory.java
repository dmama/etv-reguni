package ch.vd.unireg.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition des états "En faillite", "En liquidation", "Radiée RC" et "Fondée" à l'état "Inscrite RC"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToInscriteRCTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	public ToInscriteRCTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, EvenementFiscalService evenementFiscalService) {
		super(tiersDAO, evenementFiscalService);
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (actuel == null) {
			return new ToInscriteRCTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation, getEvenementFiscalService());
		}
		if (!checkDateValid(actuel, date)) {
			return null;
		}
		switch (actuel.getType()) {
		case FONDEE:
		case EN_LIQUIDATION:
		case RADIEE_RC:
		case EN_FAILLITE:
			return new ToInscriteRCTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation, getEvenementFiscalService());
		default:
			return null;
		}
	}
}
