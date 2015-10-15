package ch.vd.uniregctb.evenement.organisation.interne.formejuridique;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Detection du changement de forme juridique et action.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueStrategy extends AbstractOrganisationStrategy {

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		// On ne s'occupe que d'entités déjà connues
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise == null) {
			return null;
		}

		final FormeLegale formeLegaleAvant = organisation.getFormeLegale(dateAvant);
		final FormeLegale formeLegaleApres = organisation.getFormeLegale(dateApres);

		if (formeLegaleAvant != formeLegaleApres) {

			final CategorieEntreprise categoryAvant = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateAvant);
			final CategorieEntreprise categoryApres = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateApres);

			if (categoryAvant == CategorieEntreprise.APM && categoryApres == CategorieEntreprise.PM) {
				return new ChangementCategorieAPMVersPM(event, organisation, entreprise, context, options);
			} else
			if (categoryAvant != null && categoryAvant == categoryApres) {
				return new ChangementNeutreFormeJuridique(event, organisation, entreprise, context, options);
			}

			return new TraitementManuel(event, organisation, null, context, options,
			                            String.format("Changement de catégorie d'entreprise non pris en charge: %s vers %s. Veuillez traiter manuellement.",
			                                          defaultCategorie(categoryAvant), defaultCategorie(categoryApres))
			);
		}
		return null;
	}

	private String defaultCategorie(CategorieEntreprise categorie) {
		return categorie != null ? categorie.name() : "[hors catégories]";
	}
}
