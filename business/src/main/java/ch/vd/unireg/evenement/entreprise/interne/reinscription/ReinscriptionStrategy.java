package ch.vd.unireg.evenement.entreprise.interne.reinscription;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-11.
 */
public class ReinscriptionStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ReinscriptionStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);

		if (etablissementPrincipalAvantRange == null) {
			if (isExisting(entrepriseCivile, dateApres)) {
				final String message = String.format("Etablissement civil principal introuvable sur entreprise civile n°%s en date du %s", entrepriseCivile.getNumeroEntreprise(), RegDateHelper.dateToDisplayString(dateAvant));
				context.audit.info(event.getId(), message);
				return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
			}
		} else {
			final EtablissementCivil etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final EtablissementCivil etablissementPrincipalApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload();

			final InscriptionRC rcAvant = etablissementPrincipalAvant.getDonneesRC().getInscription(dateAvant);
			final InscriptionRC rcApres = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);
			final RegDate dateRadiationRCApres = rcApres != null ? rcApres.getDateRadiationCH() : null;
			final StatusInscriptionRC statusInscriptionAvant = rcAvant != null ? rcAvant.getStatus() : null;
			final StatusInscriptionRC statusInscriptionApres = rcApres != null ? rcApres.getStatus() : null;

			if (statusInscriptionAvant == StatusInscriptionRC.RADIE && (statusInscriptionApres == StatusInscriptionRC.ACTIF || statusInscriptionApres == StatusInscriptionRC.EN_LIQUIDATION)) {
				final String message = String.format("Réinscription au RC de l'entreprise n°%s (civil: %d).%s",
				                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
				                                     entrepriseCivile.getNumeroEntreprise(),
				                                     dateRadiationRCApres != null ? " Cependant, l'ancienne date de radiation persiste dans RCEnt: " + RegDateHelper.dateToDisplayString(dateRadiationRCApres) + "." : "");
				context.audit.info(event.getId(), message);
				return new Reinscription(event, entrepriseCivile, entreprise, context, options);
			}
		}
		return null;
	}
}
