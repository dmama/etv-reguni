package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseAbortException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;

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
 *         la phase initiale, en utilisant une {@link ValideurDebutDeTraitementException} plutôt qu'une {@link EvenementEntrepriseException}</li>
 * </ul>
 */
public class ValideurDebutDeTraitement extends EvenementEntrepriseInterneDeTraitement {

	/**
	 * Exception lancée dans le cadre de la validation de debut de traitement et qui demande implicitement de
	 * conserver les messages du traitement.
	 */
	public static class ValideurDebutDeTraitementException extends EvenementEntrepriseAbortException {
		public ValideurDebutDeTraitementException(String message) {
			super(message);
		}
	}

	public ValideurDebutDeTraitement(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                                 EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(evenement, entrepriseCivile, entreprise, context, options);
	}

	@Override
	public String describe() {
		return null;        // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		// rien à signaler, c'est plus tard qu'on va se manifester...
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		// Vérification que l'événement n'est pas un événement de correction, c'est à dire un événement qui survient avant le dernier événement de l'historique
		// des données civiles tel qu'on le connait actellement.
		checkEvenementDansPasse(getEvenement(), getEntrepriseCivile());

		// On doit connaître la catégorie pour continuer en mode automatique
		final EntrepriseCivile entrepriseCivile = getEntrepriseCivile();
		final RegDate dateEvt = getDateEvt();
		if (getEntreprise() == null && (!entrepriseCivile.isSocieteIndividuelle(dateEvt) && !entrepriseCivile.isSocieteSimple(dateEvt))) {
			// SIFISC-19332 - On contrôle si on existe avant, où et depuis quand. Si cela fait trop longtemps sur Vaud, c'est qu'on a un problème d'identification.
			verifieNonPreexistanteVD();
		}

	}

	/**
	 * Protection contre les événements dans le passé.
	 * Si on trouve le flag de correction dans le passé sur l'événement, ou si la date de valeur de l'événement reçu est antérieure à celle du dernier événement,
	 * on lève une erreur.
	 *
	 * @param event l'événement à inspecter
	 * @param entrepriseCivile l'entreprise civile concernée, dont le numéro d'entreprise civile doit correspondre à celui stocké dans l'événement
	 * @throws EvenementEntrepriseException au cas où on est en présence d'un événement dans le passé
	 */
	protected void checkEvenementDansPasse(EvenementEntreprise event, EntrepriseCivile entrepriseCivile) throws EvenementEntrepriseException {

		final String message = "Correction dans le passé: l'événement n°%d [%s] reçu de RCEnt pour l'entreprise civile %d %s. Traitement automatique impossible.";
		final String fragmentMessage;

		if (event.getCorrectionDansLePasse()) {
			fragmentMessage = "est marqué comme événement de correction dans le passé";
			throw new EvenementEntrepriseException(
					String.format(message, event.getNoEvenement(), RegDateHelper.dateToDisplayString(event.getDateEvenement()), entrepriseCivile.getNumeroEntreprise(), fragmentMessage)
			);
		}

		final boolean evenementDateValeurDansLePasse = getContext().getEvenementEntrepriseService().isEvenementDateValeurDansLePasse(event);
		if (evenementDateValeurDansLePasse) {
			fragmentMessage = "possède une date de valeur antérieure à la date portée par un autre événement reçu avant";
			throw new EvenementEntrepriseException(
					String.format(message, event.getNoEvenement(), RegDateHelper.dateToDisplayString(event.getDateEvenement()), entrepriseCivile.getNumeroEntreprise(), fragmentMessage)
			);
		}
	}

	// SIFISC-19332 - On contrôle si on existe avant, où et depuis quand. Si cela fait trop longtemps sur Vaud, c'est qu'on a un problème d'identification.
	protected void verifieNonPreexistanteVD() throws EvenementEntrepriseException {
		final RegDate datePasseeTropAncienne = getDateEvt().getOneDayBefore().addDays(-EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);
		// On a besoin du vrai historique pour savoir cela.
		final EntrepriseCivile entrepriseCivileHistory = getContext().getServiceEntreprise().getEntrepriseHistory(getEntrepriseCivile().getNumeroEntreprise());
		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivileHistory.getEtablissementPrincipal(datePasseeTropAncienne);
		if (etablissementPrincipalAvantRange != null) {
			EtablissementCivil etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final Domicile domicilePasse = etablissementPrincipalAvant.getDomicile(datePasseeTropAncienne);
			if (domicilePasse != null) {
				if (domicilePasse.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					final Commune commune = getCommune(domicilePasse.getNumeroOfsAutoriteFiscale(), datePasseeTropAncienne);
					throw new ValideurDebutDeTraitementException(
							String.format(
									"L'entreprise civile n°%d est présente sur Vaud (%s) depuis plus de %d jours et devrait être déjà connue d'Unireg. L'identification n'a probablement pas fonctionné. Veuillez traiter le cas à la main.",
									entrepriseCivileHistory.getNumeroEntreprise(), commune != null ? commune.getNomOfficielAvecCanton() : "",
									EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC)
					);
				}
			}
		}
	}
}
