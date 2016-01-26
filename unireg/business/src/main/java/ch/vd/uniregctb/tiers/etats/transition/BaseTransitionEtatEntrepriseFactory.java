package ch.vd.uniregctb.tiers.etats.transition;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * @author Raphaël Marmier, 2016-01-22, <raphael.marmier@vd.ch>
 */
public abstract class BaseTransitionEtatEntrepriseFactory implements TransitionEtatEntrepriseFactory {
	private final TiersDAO tiersDAO;
	protected ServiceOrganisationService serviceOrganisation;

	public BaseTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, ServiceOrganisationService serviceOrganisation) {
		this.tiersDAO = tiersDAO;
		this.serviceOrganisation = serviceOrganisation;
	}

	protected TiersDAO getTiersDAO() {
		return tiersDAO;
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
		return actuel.getDateObtention().isBeforeOrEqual(date);
	}

	/**
	 * Contrôle que l'entreprise est inscrite au RC selon RCEnt.
	 */
	protected boolean isInscriteRC(Entreprise entreprise, RegDate date) {
		if (entreprise.isConnueAuCivil()) {
			Organisation organisation = serviceOrganisation.getOrganisationHistory(entreprise.getNumeroEntreprise());
			if (organisation.getSitePrincipal(date).getPayload().getDonneesRC().getStatusInscription(date) == StatusInscriptionRC.ACTIF) {
				return true;
			}
		}
		return false;
	}
}
