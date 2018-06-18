package ch.vd.unireg.evenement.organisation.interne.reinscription;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-11.
 */
public class ReinscriptionStrategy extends AbstractOrganisationStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ReinscriptionStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement organisation reçu de RCEnt
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = organisation.getEtablissementPrincipal(dateAvant);

		if (etablissementPrincipalAvantRange == null) {
			if (isExisting(organisation, dateApres)) {
				final String message = String.format("Etablissement civil principal introuvable sur organisation n°%s en date du %s", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateAvant));
				Audit.info(event.getId(), message);
				return new TraitementManuel(event, organisation, entreprise, context, options, message);
			}
		} else {
			final EtablissementCivil etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final EtablissementCivil etablissementPrincipalApres = organisation.getEtablissementPrincipal(dateApres).getPayload();

			final InscriptionRC rcAvant = etablissementPrincipalAvant.getDonneesRC().getInscription(dateAvant);
			final InscriptionRC rcApres = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);
			final RegDate dateRadiationRCApres = rcApres != null ? rcApres.getDateRadiationCH() : null;
			final StatusInscriptionRC statusInscriptionAvant = rcAvant != null ? rcAvant.getStatus() : null;
			final StatusInscriptionRC statusInscriptionApres = rcApres != null ? rcApres.getStatus() : null;

			if (statusInscriptionAvant == StatusInscriptionRC.RADIE && (statusInscriptionApres == StatusInscriptionRC.ACTIF || statusInscriptionApres == StatusInscriptionRC.EN_LIQUIDATION)) {
				final String message = String.format("Réinscription au RC de l'entreprise n°%s (civil: %d).%s",
				                                    FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
				                                    organisation.getNumeroOrganisation(),
				                                    dateRadiationRCApres != null ? " Cependant, l'ancienne date de radiation persiste dans RCEnt: " + RegDateHelper.dateToDisplayString(dateRadiationRCApres) + "." : "");
				Audit.info(event.getId(), message);
				return new Reinscription(event, organisation, entreprise, context, options);
			}
		}
		return null;
	}
}
