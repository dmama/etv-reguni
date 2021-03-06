package ch.vd.unireg.evenement.entreprise.interne.radiation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.entreprise.interne.HandleStatus;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
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
