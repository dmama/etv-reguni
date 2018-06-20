package ch.vd.unireg.evenement.entreprise.interne.doublon;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
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
import ch.vd.unireg.tiers.Entreprise;

/**
 * Doublon d'entreprise, l'entreprise civile est remplacée par une autre.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEntrepriseRemplaceeParStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DoublonEntrepriseRemplaceeParStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
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

		final Long remplaceEtablissementAvant;
		final Long remplaceEtablissementApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload().getIdeRemplacePar(dateApres);

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {
			remplaceEtablissementAvant = etablissementPrincipalAvantRange.getPayload().getIdeRemplacePar(dateAvant);
		}
		else {
			remplaceEtablissementAvant = null;
		}

		if (remplaceEtablissementAvant == null && remplaceEtablissementApres != null) {
			final Entreprise entrepriseRemplacante;
			final Long noEntrepriseCivileRemplacante = context.getServiceEntreprise().getNoEntrepriseCivileFromNoEtablissementCivil(remplaceEtablissementApres);
			if (noEntrepriseCivileRemplacante != null) {
				entrepriseRemplacante = context.getTiersService().getEntrepriseByNoEntrepriseCivile(noEntrepriseCivileRemplacante);
			}
			else {
				entrepriseRemplacante = null;
			}


			final String message = String.format("Doublon d’entreprise civile à l'IDE. Cette entreprise n°%s (civil: %d) est remplacée par l'entreprise %s (civil: %d).",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     entrepriseCivile.getNumeroEntreprise(),
			                                     entrepriseRemplacante == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplacante.getNumero()),
			                                     noEntrepriseCivileRemplacante);
			Audit.info(event.getId(), message);
			return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, "Traitement manuel requis: " + message);
		}

		return null;
	}
}
