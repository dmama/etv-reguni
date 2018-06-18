package ch.vd.unireg.evenement.organisation.interne.radiation;

import ch.vd.registre.base.date.RegDate;
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
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-10.
 */
public class RadiationStrategy extends AbstractOrganisationStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public RadiationStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
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

		final RegDate dateApres = event.getDateEvenement();
		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();

		final EtablissementCivil etablissementPrincipalAvant = getEtablissementCivilPrincipal(organisation, dateAvant);
		if (etablissementPrincipalAvant != null) {

			final EtablissementCivil etablissementPrincipalApres = getEtablissementCivilPrincipal(organisation, dateApres);

			final boolean enCoursDeRadiationRC = etablissementPrincipalAvant.isConnuInscritAuRC(dateAvant) && !etablissementPrincipalAvant.isRadieDuRC(dateAvant) && etablissementPrincipalApres.isRadieDuRC(dateApres);

			final FormeLegale formeLegale = organisation.getFormeLegale(dateApres);

			if (enCoursDeRadiationRC) {
				final InscriptionRC inscriptionRC = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);
				final RegDate dateRadiation = inscriptionRC != null ? inscriptionRC.getDateRadiationCH() : null;
				if (dateRadiation == null) {
					final String message = String.format("Traitement manuel requis: l'entreprise n°%s est radiée du RC mais la date de radiation est introuvable!",
					                                    FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
					Audit.info(event.getId(), message);
					return new TraitementManuel(event, organisation, entreprise, context, options, message);
				}
				else {
					if (formeLegale == FormeLegale.N_0109_ASSOCIATION || formeLegale == FormeLegale.N_0110_FONDATION) {
						Audit.info(event.getId(), String.format("Radiation de l'association n°%s (%s).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), formeLegale.getLibelle()));
						return new RadiationAssociationFondation(event, organisation, entreprise, context, options, dateRadiation);
					}
					else {
						Audit.info(event.getId(), String.format("Radiation de l'entreprise n°%s (%s).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), formeLegale.getLibelle()));
						return new Radiation(event, organisation, entreprise, context, options, dateRadiation);
					}
				}
			}
		}

		return null;
	}

	protected EtablissementCivil getEtablissementCivilPrincipal(Organisation organisation, RegDate dateAvant) {
		final DateRanged<EtablissementCivil> etablissementPrincipal = organisation.getEtablissementPrincipal(dateAvant);
		return etablissementPrincipal == null ? null : etablissementPrincipal.getPayload();
	}
}
