package ch.vd.uniregctb.evenement.organisation.interne.formejuridique;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Detection du changement de forme juridique et action.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChangementFormeJuridiqueStrategy.class);
	private static final String MESSAGE_FORME_JURIDIQUE_MANQUANTE = "Forme juridique introuvable sur organisation %s en date du %s";

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
		if (entreprise == null) {
			return null;
		}

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {

			final FormeLegale formeLegaleAvant = organisation.getFormeLegale(dateAvant);
			final FormeLegale formeLegaleApres = organisation.getFormeLegale(dateApres);

			if (formeLegaleAvant == null) {
				if (isExisting(organisation, dateApres)) {
					return new TraitementManuel(event, organisation, entreprise, context, options,
							String.format(MESSAGE_FORME_JURIDIQUE_MANQUANTE, organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateAvant)));
				}
				LOGGER.info("Organisation nouvellement connue au civil. Pas de changement de catégorie.");
				return null;
			}
			else if (formeLegaleApres == null) {
				return new TraitementManuel(event, organisation, entreprise, context, options,
						String.format(MESSAGE_FORME_JURIDIQUE_MANQUANTE, organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateApres)));
			}
			else if (formeLegaleAvant != formeLegaleApres) {

				final CategorieEntreprise categoryAvant = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateAvant);
				final CategorieEntreprise categoryApres = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateApres);

				if (categoryAvant == CategorieEntreprise.APM && categoryApres == CategorieEntreprise.PM) {
					LOGGER.info("La forme juridique passe de {} à {}. Changement de catégorie: APM -> PM. ", formeLegaleAvant, formeLegaleApres);
					return new ChangementCategorieAPMVersPM(event, organisation, entreprise, context, options);
				}
				else if (categoryAvant != null && categoryAvant == categoryApres) {
					LOGGER.info("La forme juridique passe de {} à {}. Pas de changement de catégorie.", formeLegaleAvant, formeLegaleApres);
					return new ChangementNeutreFormeJuridique(event, organisation, entreprise, context, options);
				}

				return new TraitementManuel(event, organisation, null, context, options,
				                            String.format(
						                            "La forme juridique passe de %s à %s, entraînant un changement de catégorie d'entreprise non pris en charge: %s vers %s. Veuillez traiter manuellement.",
						                            formeLegaleAvant, formeLegaleApres, defaultCategorie(categoryAvant), defaultCategorie(categoryApres))
				);
			}
			LOGGER.info("La forme juridique n'a pas changée. Avant: {}, après: {}. Pas de changement de catégorie.", formeLegaleAvant, formeLegaleApres);
		}

		LOGGER.info("La forme juridique n'a pas changée.");
		return null;
	}

	private String defaultCategorie(CategorieEntreprise categorie) {
		return categorie != null ? categorie.name() : "[hors catégories]";
	}
}
