package ch.vd.unireg.evenement.organisation.interne.radiation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class RadiationAssociationFondation extends EvenementOrganisationInterneDeTraitement {

	private final RegDate dateApres;
	private final RegDate dateRadiation;

	protected RadiationAssociationFondation(EvenementOrganisation evenement, Organisation organisation,
	                                        Entreprise entreprise, EvenementOrganisationContext context,
	                                        EvenementOrganisationOptions options,
	                                        RegDate dateRadiation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		this.dateRadiation = dateRadiation;

		dateApres = evenement.getDateEvenement();

	}

	@Override
	public String describe() {
		return "Radiation APM";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
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
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		if (!getOrganisation().isRadieeDuRC(dateApres)) {
			throw new IllegalArgumentException("L'organisation n'est pas radiée du RC!");
		}
		if (dateRadiation == null) {
			throw new IllegalArgumentException("Date de radiation introuvable!");
		}

		final FormeLegale formeLegale = getOrganisation().getFormeLegale(dateApres);
		if (formeLegale != FormeLegale.N_0109_ASSOCIATION && formeLegale != FormeLegale.N_0110_FONDATION) {
			throw new IllegalArgumentException(String.format("Mauvais type d'entreprise: %s (erreur de programmation).", formeLegale.getLibelle()));
		}
	}
}
