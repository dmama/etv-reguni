package ch.vd.uniregctb.evenement.organisation.interne.helper;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;

import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.APM;
import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.DP_APM;
import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.DP_PM;
import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.FDS_PLAC;
import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.PM;
import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.PP;
import static ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise.SP;

/**
 * @author Raphaël Marmier, 2015-09-08
 */
public class CategorieEntrepriseHelper {

	/**
	 * Détermine la catégorie métier en fonction de la forme juridique.
	 *
	 * Renvoie null si la catégorie est indéterminée. C'est-à-dire si l'un ou l'autre de:
	 * - La forme légale ne correspond à aucune catégorie (On accepte les catégories inconnues)
	 * - La forme légale n'a pas été fournie en entrée (null)
	 *
	 * @param formeLegale
	 * @return la catégorie, ou null si pas de correspondance ou pas forme légale en entrée.
	 */
	public static CategorieEntreprise map(@Nullable FormeLegale formeLegale) {
		if (formeLegale != null) {
			switch (formeLegale) {
			/* Personne Physique */
			case N_0101_ENTREPRISE_INDIVIDUELLE:
				return PP;
			/* Société de personnes */
			case N_0103_SOCIETE_NOM_COLLECIF:
			case N_0104_SOCIETE_EN_COMMANDITE:
				return SP;
			/* Personne morale */
			case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
			case N_0106_SOCIETE_ANONYME:
			case N_0107_SOCIETE_A_RESPONSABILITE_LIMITE:
			case N_0108_SOCIETE_COOPERATIVE:
				return PM;
			/* Association et Fondation */
			case N_0109_ASSOCIATION:
			case N_0110_FONDATION:
				return APM;
			/* Fonds de placement */
			case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
			case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
			case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
				return FDS_PLAC;
			/* PM de droit public */
			case N_0117_INSTITUT_DE_DROIT_PUBLIC:
			case N_0220_ADMINISTRATION_CONFEDERATION:
			case N_0221_ADMINISTRATION_CANTON:
			case N_0222_ADMINISTRATION_DISTRICT:
			case N_0223_ADMINISTRATION_COMMUNE:
			case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
				return DP_APM;
			/* APM de droit public */
			case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
				return DP_PM;
			default:
				return null;
			}
		}
		return null;
	}

	// TODO: Déplacer dans l'adapter?
	@Nullable
	public static CategorieEntreprise getCategorieEntreprise(Organisation organisation, RegDate date) {
		return CategorieEntrepriseHelper.map(organisation.getFormeLegale(date));
	}
}
