package ch.vd.unireg.evenement.organisation.interne.radiation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-10.
 */
public class RadiationStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public RadiationStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
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

		final RegDate dateApres = event.getDateEvenement();
		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();

		final EtablissementCivil etablissementPrincipalAvant = getEtablissementCivilPrincipal(entrepriseCivile, dateAvant);
		if (etablissementPrincipalAvant != null) {

			final EtablissementCivil etablissementPrincipalApres = getEtablissementCivilPrincipal(entrepriseCivile, dateApres);

			final boolean enCoursDeRadiationRC = etablissementPrincipalAvant.isConnuInscritAuRC(dateAvant) && !etablissementPrincipalAvant.isRadieDuRC(dateAvant) && etablissementPrincipalApres.isRadieDuRC(dateApres);

			final FormeLegale formeLegale = entrepriseCivile.getFormeLegale(dateApres);

			if (enCoursDeRadiationRC) {
				final InscriptionRC inscriptionRC = etablissementPrincipalApres.getDonneesRC().getInscription(dateApres);
				final RegDate dateRadiation = inscriptionRC != null ? inscriptionRC.getDateRadiationCH() : null;
				if (dateRadiation == null) {
					final String message = String.format("Traitement manuel requis: l'entreprise n°%s est radiée du RC mais la date de radiation est introuvable!",
					                                    FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
					Audit.info(event.getId(), message);
					return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
				}
				else {
					if (formeLegale == FormeLegale.N_0109_ASSOCIATION || formeLegale == FormeLegale.N_0110_FONDATION) {
						Audit.info(event.getId(), String.format("Radiation de l'association n°%s (%s).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), formeLegale.getLibelle()));
						return new RadiationAssociationFondation(event, entrepriseCivile, entreprise, context, options, dateRadiation);
					}
					else {
						Audit.info(event.getId(), String.format("Radiation de l'entreprise n°%s (%s).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), formeLegale.getLibelle()));
						return new Radiation(event, entrepriseCivile, entreprise, context, options, dateRadiation);
					}
				}
			}
		}

		return null;
	}

	protected EtablissementCivil getEtablissementCivilPrincipal(EntrepriseCivile entrepriseCivile, RegDate dateAvant) {
		final DateRanged<EtablissementCivil> etablissementPrincipal = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		return etablissementPrincipal == null ? null : etablissementPrincipal.getPayload();
	}
}
