package ch.vd.uniregctb.evenement.organisation.interne.formejuridique;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * Detection du changement de forme juridique et action.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChangementFormeJuridiqueStrategy.class);
	private static final String MESSAGE_FORME_JURIDIQUE_MANQUANTE = "Forme juridique introuvable sur organisation %s en date du %s";

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ChangementFormeJuridiqueStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

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
				LOGGER.info("Organisation nouvellement connue au civil. Pas de changement de forme juridique.");
				return null;
			}
			else if (formeLegaleApres == null) {
				return new TraitementManuel(event, organisation, entreprise, context, options,
						String.format(MESSAGE_FORME_JURIDIQUE_MANQUANTE, organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateApres)));
			}
			else if (formeLegaleAvant != formeLegaleApres) {

				/*
					On prend comme point de départ le type de régime fiscal du régime de portée VD de l'entreprise à la veille, s'il existe.
				 */
				final TypeRegimeFiscal typeRegimeFiscalVDAvant = context.getRegimeFiscalService().getTypeRegimeFiscalVD(entreprise, dateAvant);

				if (typeRegimeFiscalVDAvant == null) {
					return new TraitementManuel(event, organisation, null, context, options,
					                            String.format(
							                            "La forme juridique passe de %s à %s. Cependant, on ne trouve pas en date du %s de régime fiscal de portée VD sur l'entreprise n°%s correspondante. Impossible de statuer. Veuillez traiter manuellement.",
							                            formeLegaleAvant, formeLegaleApres, RegDateHelper.dateToDisplayString(dateAvant), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()))
					);
				}

				/*
					Le type de régime fiscal à l'arrivée est déterminé exclusivement par le type par défaut en fonction de la forme juridique civile.
				 */
				final TypeRegimeFiscal typeRegimeFiscalParDefautApres = context.getRegimeFiscalService().getTypeRegimeFiscalParDefaut(FormeJuridiqueEntreprise.fromCode(formeLegaleApres.getCode()));

				/*
					Pas de changement de régime. Annoncer comme neutre.
				 */
				if (typeRegimeFiscalVDAvant.getCode().equals(typeRegimeFiscalParDefautApres.getCode())) {
					final String message = String.format("La forme juridique passe de %s à %s. Le régime fiscal VD reste %s.", formeLegaleAvant, formeLegaleApres, typeRegimeFiscalParDefautApres.getLibelleAvecCode());
					LOGGER.info(message);
					return new ChangementNeutreFormeJuridique(event, organisation, entreprise, context, options);
				}

				/*
					Le type de régime fiscal par défaut au départ permettra de savoir si la valeur de départ a été ajustée
				 */
				final TypeRegimeFiscal typeRegimeFiscalParDefautAvant = context.getRegimeFiscalService().getTypeRegimeFiscalParDefaut(FormeJuridiqueEntreprise.fromCode(formeLegaleAvant.getCode()));

				/*
					A-t-on fonctionné en mode automatique lors du réglage du précédent régime. Ca ne compte pas si on est resté en type indéterminé. On doit avoir un vrai régime.
				 */
				final boolean wasAuto = ComparisonHelper.areEqual(typeRegimeFiscalVDAvant.getCode(), typeRegimeFiscalParDefautAvant.getCode()) && typeRegimeFiscalParDefautAvant.getCategorie() != CategorieEntreprise.INDET;

				/*
					Le régime fiscal avait été attribué automatiquement. Si c'est bon une fois, c'est bon deux fois, avec au bout du chemin un type indéterminé si la nouvelle forme juridique devait être exotique.
					Ca va aussi si le type par défaut du futur régime est indéterminé, car cela provoquera l'intervention d'un opérateur.
				 */
				if (wasAuto || typeRegimeFiscalParDefautApres.isIndetermine()) {
					final String message = String.format("La forme juridique passe de %s à %s. Le régime fiscal VD passe de %s à %s.",
					                                     formeLegaleAvant, formeLegaleApres, typeRegimeFiscalVDAvant.getLibelleAvecCode(), typeRegimeFiscalParDefautApres.getLibelleAvecCode());
					LOGGER.info(message);
					return new ChangementRegimeFiscalParDefaut(event, organisation, entreprise, context, options);
				}

				/*
					Changement de régime fiscal avec intervention manuelle. (L'ancien régime avait été attribué à la main)
				 */
				final String message = String.format(
						"La forme juridique passe de %s à %s. Le régime fiscal VD passerait de %s à %s. Comme le précédent régime fiscal avait été attribué à la main, le nouveau est réglé comme indéterminé.",
						formeLegaleAvant, formeLegaleApres, typeRegimeFiscalVDAvant.getLibelleAvecCode(), typeRegimeFiscalParDefautApres.getLibelleAvecCode());
				LOGGER.info(message);
				return new ChangementRegimeFiscalIndetermine(event, organisation, entreprise, context, options);

			}
			LOGGER.info("La forme juridique n'a pas changée (avant: {}, après: {}). Le régime fiscal ne change pas.", formeLegaleAvant, formeLegaleApres);
			return null;
		}

		LOGGER.info("La forme juridique n'a pas changée. (Pas de site au civil avant.)");
		return null;
	}
}
