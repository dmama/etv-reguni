package ch.vd.uniregctb.tiers;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Mickaël Zanoli, 2016-02-23
 */
public class FormeJuridiqueEntrepriseHelper {

	/**
	 * Retourne une liste de formes juridiques pour une catégorie d'entreprise
	 * Ne retourne jamais null mais au pire une liste vide
	 *
	 * @param categorieEntreprise la catégorie d'entreprise concernée
	 * @return la liste des formes juridiques associées
	 */
	public static EnumSet<FormeJuridiqueEntreprise> getFormesJuridiquesFromCategorieEntreprise(@Nullable CategorieEntreprise categorieEntreprise) {
		EnumSet<FormeJuridiqueEntreprise> result = EnumSet.noneOf(FormeJuridiqueEntreprise.class);
		if (categorieEntreprise != null) {
			for (FormeJuridiqueEntreprise formeJuridique : FormeJuridiqueEntreprise.values()) {
				if (CategorieEntrepriseHelper.map(formeJuridique) == categorieEntreprise) {
					result.add(formeJuridique);
				}
			}
		}
		return result;
	}

	/**
	 * Retourne une liste de formes juridiques pour des catégories d'entreprise
	 * Ne retourne jamais null mais au pire une liste vide
	 *
	 * @param listeCategoriesEntreprise la liste des catégories d'entreprise concernées
	 * @return la liste des formes juridiques associées
	 */
	public static EnumSet<FormeJuridiqueEntreprise> getFormesJuridiquesFromCategoriesEntreprise(EnumSet<CategorieEntreprise> listeCategoriesEntreprise) {
		EnumSet<FormeJuridiqueEntreprise> result = EnumSet.noneOf(FormeJuridiqueEntreprise.class);
		for (CategorieEntreprise categorie : listeCategoriesEntreprise) {
			result.addAll(getFormesJuridiquesFromCategorieEntreprise(categorie));
		}
		return result;
	}
}
