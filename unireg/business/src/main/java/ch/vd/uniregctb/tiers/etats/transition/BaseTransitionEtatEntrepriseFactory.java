package ch.vd.uniregctb.tiers.etats.transition;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * @author Raphaël Marmier, 2016-01-22, <raphael.marmier@vd.ch>
 */
public abstract class BaseTransitionEtatEntrepriseFactory implements TransitionEtatEntrepriseFactory {
	private final TiersDAO tiersDAO;
	private final EvenementFiscalService evenementFiscalService;



	public BaseTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, EvenementFiscalService evenementFiscalService) {
		this.tiersDAO = tiersDAO;
		this.evenementFiscalService = evenementFiscalService;
	}

	protected TiersDAO getTiersDAO() {
		return tiersDAO;
	}
	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	/**
	 * Renvoie l'état actuellement en vigueur pour l'entreprise
	 * @param entreprise l'entreprise concernée
	 * @return l'état en vigueur
	 */
	protected static EtatEntreprise getEtatActuel(@NotNull Entreprise entreprise) {
		return entreprise.getEtatActuel();
	}

	/**
	 * S'assure que la date passée pour une transition d'état soit compatible avec l'état actuel, qui doit être
	 * chronologiquement antérieur à un nouvel état démarrant à la date
	 *
	 * @param actuel l'état actuel
	 * @param date la date de démarrage d'un nouvel état
	 * @return true si le nouvel état peut commencer à la date, false sinon
	 */
	protected static boolean checkDateValid(EtatEntreprise actuel, RegDate date) {
		return actuel == null || actuel.getDateObtention().isBeforeOrEqual(date);
	}
}
