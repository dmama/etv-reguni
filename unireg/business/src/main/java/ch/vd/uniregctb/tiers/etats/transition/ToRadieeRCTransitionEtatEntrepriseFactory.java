package ch.vd.uniregctb.tiers.etats.transition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;

/**
 * Factory produisant une transition des états "En faillite", "En liquidation" et "Absorbée" à l'état "Radiée RC"
 *
 * @author Raphaël Marmier, 2016-01-25, <raphael.marmier@vd.ch>
 */
public class ToRadieeRCTransitionEtatEntrepriseFactory extends BaseTransitionEtatEntrepriseFactory {

	private ServiceOrganisationService serviceOrganisation;

	public ToRadieeRCTransitionEtatEntrepriseFactory(TiersDAO tiersDAO, ServiceOrganisationService serviceOrganisation) {
		super(tiersDAO);
		this.serviceOrganisation = serviceOrganisation;
	}

	@Override
	public TransitionEtatEntreprise create(Entreprise entreprise, RegDate date, TypeGenerationEtatEntreprise generation) {
		final EtatEntreprise actuel = getEtatActuel(entreprise);
		if (checkDateValid(actuel, date)) {
			switch (actuel.getType()) {
			case EN_FAILLITE:
			case EN_LIQUIDATION:
				return new ToRadieeRCTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
			case ABSORBEE:
				if (isInscriteRC(entreprise, date)) {
					return new ToRadieeRCTransitionEtatEntreprise(getTiersDAO(), entreprise, date, generation);
				}
			}
		}
		return null;
	}

	/**
	 * Contrôle que l'entreprise est inscrite au RC selon RCEnt.
	 */
	private boolean isInscriteRC(Entreprise entreprise, RegDate date) {
		if (entreprise.isConnueAuCivil()) {
			Organisation organisation = serviceOrganisation.getOrganisationHistory(entreprise.getNumeroEntreprise());
			if (organisation.getSitePrincipal(date).getPayload().getDonneesRC().getStatusInscription(date) == StatusInscriptionRC.ACTIF) {
				return true;
			}
		}
		return false;
	}
}
