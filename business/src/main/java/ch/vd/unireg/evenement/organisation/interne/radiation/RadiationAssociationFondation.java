package ch.vd.unireg.evenement.organisation.interne.radiation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class RadiationAssociationFondation extends EvenementEntrepriseInterneDeTraitement {

	private final RegDate dateApres;
	private final RegDate dateRadiation;

	protected RadiationAssociationFondation(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                                        Entreprise entreprise, EvenementEntrepriseContext context,
	                                        EvenementEntrepriseOptions options,
	                                        RegDate dateRadiation) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);

		this.dateRadiation = dateRadiation;

		dateApres = evenement.getDateEvenement();

	}

	@Override
	public String describe() {
		return "Radiation APM";
	}


	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		boolean assujettie = isAssujettie(getEntreprise(), dateRadiation);

		if (assujettie) {
			if (getEntreprise().getEtatActuel().getType() == TypeEtatEntreprise.INSCRITE_RC) {
				changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.RADIEE_RC, dateRadiation, suivis);
				changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.FONDEE, dateRadiation, suivis);
				warnings.addWarning("Vérification requise pour la radiation de l'association / fondation encore assujettie sortie du RC.");
			}
			else if (getEntreprise().getEtatActuel().getType() == TypeEtatEntreprise.EN_FAILLITE) {
				changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.RADIEE_RC, dateRadiation, suivis);
				changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.EN_FAILLITE, dateRadiation, suivis);
				suivis.addSuivi("On considère que l'association / fondation reste en activité puisqu'elle est toujours assujettie, bien qu'elle soit en faillite.");
				warnings.addWarning("Vérification requise pour la radiation de l'association / fondation encore assujettie sortie du RC, qui reste en faillite.");
			}
		} else {
			changeEtatEntreprise(getEntreprise(), TypeEtatEntreprise.RADIEE_RC, dateRadiation, suivis);
			warnings.addWarning("Vérification requise pour l'association / fondation radiée du RC.");
		}

		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		if (!getEntrepriseCivile().isRadieeDuRC(dateApres)) {
			throw new IllegalArgumentException("L'entreprise n'est pas radiée du RC!");
		}
		if (dateRadiation == null) {
			throw new IllegalArgumentException("Date de radiation introuvable!");
		}

		final FormeLegale formeLegale = getEntrepriseCivile().getFormeLegale(dateApres);
		if (formeLegale != FormeLegale.N_0109_ASSOCIATION && formeLegale != FormeLegale.N_0110_FONDATION) {
			throw new IllegalArgumentException(String.format("Mauvais type d'entreprise: %s (erreur de programmation).", formeLegale.getLibelle()));
		}
	}
}
