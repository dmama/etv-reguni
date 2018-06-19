package ch.vd.unireg.evenement.organisation.interne.doublon;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneComposite;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Modification de capital à propager sans effet.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEtablissementStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DoublonEtablissementStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
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

		final List<EvenementEntrepriseInterne> doublons = new ArrayList<>();

		for (EtablissementCivil etablissementCivil : entrepriseCivile.getEtablissements()) {
			final Long remplaceParAvant = etablissementCivil.getIdeRemplacePar(dateAvant);
			final Long remplaceParApres = etablissementCivil.getIdeRemplacePar(dateApres);

			if (remplaceParAvant == null && remplaceParApres!= null) {

				final Etablissement etablissement = context.getTiersService().getEtablissementByNumeroEtablissementCivil(etablissementCivil.getNumeroEtablissement());
				final Etablissement etablissementRemplacant = context.getTiersService().getEtablissementByNumeroEtablissementCivil(remplaceParApres);

				final String message = String.format("Doublon d'établissement civil à l'IDE. L'établissement %s (civil: %d) est remplacé par l'établissement %s (civil: %d).",
				                                     etablissement == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
				                                     etablissementCivil.getNumeroEtablissement(),
				                                     etablissementRemplacant == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissementRemplacant.getNumero()),
				                                     remplaceParApres);
				doublons.add(new TraitementManuel(event, entrepriseCivile, entreprise, context, options, "Traitement manuel requis: " + message));
			}

			final Long enRemplacementDeAvant = etablissementCivil.getIdeEnRemplacementDe(dateAvant);
			final Long enRemplacementDeApres = etablissementCivil.getIdeEnRemplacementDe(dateApres);

			if (enRemplacementDeAvant == null && enRemplacementDeApres!= null) {

				final Etablissement etablissement = context.getTiersService().getEtablissementByNumeroEtablissementCivil(etablissementCivil.getNumeroEtablissement());
				final Etablissement etablissementRemplace = context.getTiersService().getEtablissementByNumeroEtablissementCivil(enRemplacementDeApres);

				final String message = String.format("Doublon d'établissement civil à l'IDE. L'établissement %s (civil: %d) remplace l'établissement %s (civil: %d).",
				                                     etablissement == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
				                                     etablissementCivil.getNumeroEtablissement(),
				                                     etablissementRemplace == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissementRemplace.getNumero()),
				                                     enRemplacementDeApres);
				doublons.add(new TraitementManuel(event, entrepriseCivile, entreprise, context, options, "Traitement manuel requis: " + message));
			}
		}

		if (!doublons.isEmpty()) {
			if (doublons.size() == 1) {
				Audit.info(event.getId(), "Un doublon d'établissement détecté.");
				return doublons.get(0);
			} else {
				Audit.info(event.getId(), String.format("%d doublons d'établissement détectés.", doublons.size()));
				return new EvenementEntrepriseInterneComposite(event, entrepriseCivile, entreprise, context, options, doublons);
			}
		}
		return null;
	}
}
