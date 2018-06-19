package ch.vd.unireg.evenement.organisation.interne.doublon;

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
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Doublon d'entreprise, l'entreprise civile en remplace une autre.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEntrepriseRemplacanteStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DoublonEntrepriseRemplacanteStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
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

		final Long enRemplacementEtablissementAvant;
		final Long enRemplacementEtablissementApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload().getIdeEnRemplacementDe(dateApres);

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {
			enRemplacementEtablissementAvant = etablissementPrincipalAvantRange.getPayload().getIdeEnRemplacementDe(dateAvant);
		}
		else {
			enRemplacementEtablissementAvant = null;
		}

		if (enRemplacementEtablissementAvant == null && enRemplacementEtablissementApres != null) {
			final Entreprise entrepriseRemplacee;
			final Long noEntrepriseCivileRemplacee = context.getServiceEntreprise().getNoEntrepriseCivileFromNoEtablissementCivil(enRemplacementEtablissementApres);
			if (noEntrepriseCivileRemplacee != null) {
				entrepriseRemplacee = context.getTiersService().getEntrepriseByNoEntrepriseCivile(noEntrepriseCivileRemplacee);
			}
			else {
				entrepriseRemplacee = null;
			}

			final String message = String.format("Doublon d’entreprise civile à l'IDE. Cette entreprise n°%s (civil: %d) remplace l'entreprise %s (civil: %d).",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     entrepriseCivile.getNumeroEntreprise(),
			                                     entrepriseRemplacee == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplacee.getNumero()),
			                                     noEntrepriseCivileRemplacee);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, "Traitement manuel requis: " + message);
		}

		return null;
	}
}
