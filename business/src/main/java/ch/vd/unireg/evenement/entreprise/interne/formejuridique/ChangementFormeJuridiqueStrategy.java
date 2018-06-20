package ch.vd.unireg.evenement.entreprise.interne.formejuridique;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.ComparisonHelper;
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
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/**
 * Detection du changement de forme juridique et action.
 *
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementFormeJuridiqueStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ChangementFormeJuridiqueStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {

			final FormeLegale formeLegaleAvant = entrepriseCivile.getFormeLegale(dateAvant);
			final FormeLegale formeLegaleApres = entrepriseCivile.getFormeLegale(dateApres);

			if (formeLegaleAvant == null || formeLegaleApres == null) {
				Audit.warn(event.getId(), String.format("Il manque une des deux ou les deux formes juridiques (avant: %s, après: %s). Impossible de déterminer un éventuellement changement.", formeLegaleAvant, formeLegaleApres));
				return null;
			}
			if (formeLegaleAvant != formeLegaleApres) { // Ce que l'on fait si la forme juridique est nulle est défini dans la stratégie idoine.

				// On prend comme point de départ le type de régime fiscal du régime de portée VD de l'entreprise à la veille, s'il existe.
				final TypeRegimeFiscal typeRegimeFiscalVDAvant = context.getRegimeFiscalService().getTypeRegimeFiscalVD(entreprise, dateAvant);

				if (typeRegimeFiscalVDAvant == null) {
					final String message = String.format("La forme juridique passe de %s à %s. Cependant, on ne trouve pas en date du %s " +
							                                     "de régime fiscal de portée VD sur l'entreprise n°%s correspondante. Impossible de statuer. Veuillez traiter manuellement.",
					                                     formeLegaleAvant, formeLegaleApres, RegDateHelper.dateToDisplayString(dateAvant), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()));
					Audit.info(event.getId(), message);
					return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
				}

				// Le type de régime fiscal à l'arrivée est déterminé exclusivement par le type par défaut en fonction de la forme juridique civile.
				final TypeRegimeFiscal typeRegimeFiscalParDefautApres = context.getRegimeFiscalService().getTypeRegimeFiscalParDefaut(FormeJuridiqueEntreprise.fromCode(formeLegaleApres.getCode()));

				// Pas de changement de régime. Annoncer comme neutre.
				if (typeRegimeFiscalVDAvant.getCode().equals(typeRegimeFiscalParDefautApres.getCode())) {
					final String message = String.format("La forme juridique passe de %s à %s. Le régime fiscal VD reste %s.", formeLegaleAvant, formeLegaleApres, typeRegimeFiscalParDefautApres.getLibelleAvecCode());
					Audit.info(event.getId(), message);
					return new ChangementNeutreFormeJuridique(event, entrepriseCivile, entreprise, context, options);
				}

				// Le type de régime fiscal par défaut au départ permettra de savoir si la valeur de départ a été ajustée
				final TypeRegimeFiscal typeRegimeFiscalParDefautAvant = context.getRegimeFiscalService().getTypeRegimeFiscalParDefaut(FormeJuridiqueEntreprise.fromCode(formeLegaleAvant.getCode()));

				// A-t-on fonctionné en mode automatique lors du réglage du précédent régime. Ca ne compte pas si on est resté en type indéterminé. On doit avoir un vrai régime.
				final boolean wasAuto = ComparisonHelper.areEqual(typeRegimeFiscalVDAvant.getCode(), typeRegimeFiscalParDefautAvant.getCode()) && typeRegimeFiscalParDefautAvant.getCategorie() != CategorieEntreprise.INDET;

				// Le régime fiscal avait été attribué automatiquement. Si c'est bon une fois, c'est bon deux fois, avec au bout du chemin un type indéterminé si la nouvelle forme juridique devait être exotique.
				// Ca va aussi si le type par défaut du futur régime est indéterminé, car cela provoquera l'intervention d'un opérateur.
				if (wasAuto || typeRegimeFiscalParDefautApres.isIndetermine()) {
					final String message = String.format("La forme juridique passe de %s à %s. Le régime fiscal VD passe de %s à %s.",
					                                     formeLegaleAvant, formeLegaleApres, typeRegimeFiscalVDAvant.getLibelleAvecCode(), typeRegimeFiscalParDefautApres.getLibelleAvecCode());
					Audit.info(event.getId(), message);
					return new ChangementRegimeFiscalParDefaut(event, entrepriseCivile, entreprise, context, options);
				}

				// Changement de régime fiscal avec intervention manuelle. (L'ancien régime avait été attribué à la main)
				final String message = String.format(
						"La forme juridique passe de %s à %s. Le régime fiscal VD passerait de %s à %s. Comme le précédent régime fiscal avait été attribué à la main, le nouveau est réglé comme indéterminé.",
						formeLegaleAvant, formeLegaleApres, typeRegimeFiscalVDAvant.getLibelleAvecCode(), typeRegimeFiscalParDefautApres.getLibelleAvecCode());
				Audit.info(event.getId(), message);
				return new ChangementRegimeFiscalIndetermine(event, entrepriseCivile, entreprise, context, options);

			}
		}

		return null;
	}
}
