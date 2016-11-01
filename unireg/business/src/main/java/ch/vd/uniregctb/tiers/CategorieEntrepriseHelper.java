package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2015-09-08
 */
public class CategorieEntrepriseHelper {


	/**
	 * Détermine la catégorie métier en fonction de la forme juridique.
	 *
	 * Renvoie null si la catégorie est indéterminée. C'est-à-dire si l'un ou l'autre de:
	 * - La forme légale n'a pas été fournie en entrée (null)
	 *
	 * @param formeLegale la forme légale en provenance de RCEnt
	 * @return la catégorie, ou null si pas de correspondance ou pas forme légale en entrée.
	 */
	@NotNull
	public static CategorieEntreprise map(@NotNull FormeLegale formeLegale) {
		switch (formeLegale) {
			/* Personne Physique */
		case N_0101_ENTREPRISE_INDIVIDUELLE:
			return CategorieEntreprise.PP;
			/* Société de personnes */
		case N_0103_SOCIETE_NOM_COLLECTIF:
		case N_0104_SOCIETE_EN_COMMANDITE:
			return CategorieEntreprise.SP;
			/* Personne morale */
		case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
		case N_0106_SOCIETE_ANONYME:
		case N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE:
		case N_0108_SOCIETE_COOPERATIVE:
			return CategorieEntreprise.PM;
			/* Association et Fondation */
		case N_0109_ASSOCIATION:
		case N_0110_FONDATION:
			return CategorieEntreprise.APM;
			/* Fonds de placement */
		case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
		case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
		case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
			return CategorieEntreprise.FP;
			/* PM de droit public */
		case N_0117_INSTITUT_DE_DROIT_PUBLIC:
		case N_0220_ADMINISTRATION_CONFEDERATION:
		case N_0221_ADMINISTRATION_CANTON:
		case N_0222_ADMINISTRATION_DISTRICT:
		case N_0223_ADMINISTRATION_COMMUNE:
		case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
			return CategorieEntreprise.DPAPM;
			/* APM de droit public */
		case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
			return CategorieEntreprise.DPPM;
		default:
			return CategorieEntreprise.AUTRE;
		}
	}

	// TODO: Déplacer dans l'adapter?
	@Nullable
	public static CategorieEntreprise getCategorieEntreprise(Organisation organisation, RegDate date) {
		final FormeLegale formeLegale = organisation.getFormeLegale(date);
		return formeLegale == null ? null : CategorieEntrepriseHelper.map(formeLegale);
	}

	/**
	 * @param formeJuridique une forme juridique "fiscale"
	 * @return la catégorie d'entreprise correspondante
	 */
	@NotNull
	public static CategorieEntreprise map(@NotNull FormeJuridiqueEntreprise formeJuridique) {
		switch (formeJuridique) {
		case EI:
			return CategorieEntreprise.PP;
		case SC:
		case SNC:
			return CategorieEntreprise.SP;
		case SCA:
		case SA:
		case SARL:
		case SCOOP:
			return CategorieEntreprise.PM;
		case ASSOCIATION:
		case FONDATION:
			return CategorieEntreprise.APM;
		case SCPC:
		case SICAF:
		case SICAV:
			return CategorieEntreprise.FP;
		case IDP:
		case ADM_CH:
		case ADM_CO:
		case ADM_CT:
		case ADM_DI:
		case CORP_DP_ADM:
			return CategorieEntreprise.DPAPM;
		case CORP_DP_ENT:
			return CategorieEntreprise.DPPM;
		default:
			return CategorieEntreprise.AUTRE;
		}
	}
}
