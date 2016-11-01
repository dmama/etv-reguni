package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationAbortException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * <p>
 *     Evénement interne fictif qui sert à valider après la phase initiale, après l'évaluation des stratégies et
 *     après les mises en traitement manuel, mais avant le début des traitements effectifs (handle()) qu'on est
 *     dans un situation acceptable. Le fait de se dérouler dans le premier handle() du traitement est justifié
 *     par deux arguments:
 * </p>
 * <ul>
 *     <li>On a encore accès aux données civiles de RCEnt à ce stade. (Par opposition au valideur d'Unireg)</li>
 *     <li>On n'a pas encore vraiment commencé à traiter</li>
 *     <li>On a la possibilité de conserver les messages de traitements précédant, en particulier ceux émis par
 *         la phase initiale, en utilisant une {@link ValideurDebutDeTraitementException} plutôt qu'une {@link EvenementOrganisationException}</li>
 * </ul>
 */
public class ValideurDebutDeTraitement extends EvenementOrganisationInterneDeTraitement {

	/**
	 * Exception lancée dans le cadre de la validation de fin de traitement et qui demande implicitement de
	 * conserver les messages du traitement.
	 */
	public static class ValideurDebutDeTraitementException extends EvenementOrganisationAbortException {
		public ValideurDebutDeTraitementException(String message) {
			super(message);
		}
	}

	public ValideurDebutDeTraitement(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public String describe() {
		return null;        // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// rien à signaler, c'est plus tard qu'on va se manifester...
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

		// On doit connaître la catégorie pour continuer en mode automatique
		final Organisation organisation = getOrganisation();
		CategorieEntreprise category = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, getDateEvt());
		if (category == null) {
			throw new ValideurDebutDeTraitementException(
					String.format("Impossible de déterminer la catégorie d'entreprise de l'organisation n°%d. Veuillez traiter le cas à la main.", organisation.getNumeroOrganisation())
			);
		}
		final FormeLegale formeLegale = organisation.getFormeLegale(getDateEvt());
		if (getEntreprise() == null && (category != CategorieEntreprise.PP && formeLegale != FormeLegale.N_0302_SOCIETE_SIMPLE)) {
			// SIFISC-19332 - On contrôle si on existe avant, où et depuis quand. Si cela fait trop longtemps sur Vaud, c'est qu'on a un problème d'identification.
			verifieNonPreexistanteVD();
		}

	}

	// SIFISC-19332 - On contrôle si on existe avant, où et depuis quand. Si cela fait trop longtemps sur Vaud, c'est qu'on a un problème d'identification.
	protected void verifieNonPreexistanteVD() throws EvenementOrganisationException {
		final RegDate datePasseeTropAncienne = getDateEvt().getOneDayBefore().addDays(-OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);
		// On a besoin du vrai historique pour savoir cela.
		final Organisation organisationHistory = getContext().getServiceOrganisation().getOrganisationHistory(getOrganisation().getNumeroOrganisation());
		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisationHistory.getSitePrincipal(datePasseeTropAncienne);
		if (sitePrincipalAvantRange != null) {
			SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final Domicile domicilePasse = sitePrincipalAvant.getDomicile(datePasseeTropAncienne);
			if (domicilePasse != null) {
				if (domicilePasse.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					final Commune commune = getCommune(domicilePasse.getNumeroOfsAutoriteFiscale(), datePasseeTropAncienne);
					throw new ValideurDebutDeTraitementException(
							String.format(
									"L'organisation n°%d est présente sur Vaud (%s) depuis plus de %d jours et devrait être déjà connue d'Unireg. L'identification n'a probablement pas fonctionné. Veuillez traiter le cas à la main.",
									organisationHistory.getNumeroOrganisation(), commune != null ? commune.getNomOfficielAvecCanton() : "",
									OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC)
					);
				}
			}
		}
	}
}
