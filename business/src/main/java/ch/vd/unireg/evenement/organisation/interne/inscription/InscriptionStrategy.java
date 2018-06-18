package ch.vd.unireg.evenement.organisation.interne.inscription;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Inscription au RC
 *
 * Cette stratégie doit être évaluée AVANT la stratégie évaluant la radiation, afin que si l'entreprise
 * est inscrite et radiée le même jour, elle termine radiée du RC.
 *
 * @author Raphaël Marmier, 2016-02-23.
 */
public class InscriptionStrategy extends AbstractOrganisationStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public InscriptionStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement organisation reçu de RCEnt
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise) throws EvenementOrganisationException {

		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = organisation.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {
			final EtablissementCivil etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final InscriptionRC inscriptionAvant = etablissementPrincipalAvant.getDonneesRC().getInscription(dateAvant);
			if (inscriptionAvant == null || !inscriptionAvant.isInscrit()) {
				final RegDate dateInscriptionRCAvant = inscriptionAvant != null ? inscriptionAvant.getDateInscriptionCH() : null;
				final RegDate dateRadiationRCAvant = inscriptionAvant != null ? inscriptionAvant.getDateRadiationCH() : null;

				final EtablissementCivil etablissementPrincipalApres = organisation.getEtablissementPrincipal(dateApres).getPayload();
				final InscriptionRC inscriptionApres = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);
				final RegDate dateInscriptionRCApres = inscriptionApres != null ? inscriptionApres.getDateInscriptionCH() : null;

				if (dateInscriptionRCAvant == null && dateRadiationRCAvant == null && dateInscriptionRCApres != null) {
					Audit.info(event.getId(), String.format("Inscription au RC de l'entreprise n°%s (civil: %d).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), organisation.getNumeroOrganisation()));
					return new Inscription(event, organisation, entreprise, context, options);
				}
			}
		}

		return null;
	}
}
