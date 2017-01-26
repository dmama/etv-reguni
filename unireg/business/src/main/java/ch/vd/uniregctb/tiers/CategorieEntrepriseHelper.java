package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.CategorieEntrepriseFidor;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * @author Raphaël Marmier, 2015-09-08
 */
public class CategorieEntrepriseHelper {

	@NotNull
	public static CategorieEntreprise convert(CategorieEntrepriseFidor categorieEntreprise) {
		switch (categorieEntreprise) {
		case PM: return CategorieEntreprise.PM;
		case APM: return CategorieEntreprise.APM;
		case SP: return CategorieEntreprise.SP;
		case INDET: return CategorieEntreprise.INDET;
		default: return CategorieEntreprise.AUTRE;
		}
	}
}
