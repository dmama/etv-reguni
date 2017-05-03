package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Contient les identifiants des éléments à ne pas imnporter du registe foncier pour une raison ou une autre.
 */
public class BlacklistRFHelper {

	// [SIFISC-22366] On ignore l'unique immeuble sur la commune 'Autre Registre foncier'.
	// [SIFISC-24611] On ignore les 9 immeubles sur les communes Neuchâteloises de Montalchez et Fresens
	private static final Set<String> immeubles = new HashSet<>(Arrays.asList("_1f1091523810108101381012b3d64cb4",
	                                                                         "_1f1091523810190f0138101cd5c83f8a",
	                                                                         "_1f1091523810190f0138101cd5c83f8e",
	                                                                         "_1f1091523810190f0138101cd6404147",
	                                                                         "_1f1091523810190f0138101cd6404148",
	                                                                         "_1f1091523810190f0138101cd640414c",
	                                                                         "_1f1091523810190f0138101cd640414d",
	                                                                         "_1f1091523810190f0138101cd640414e",
	                                                                         "_1f1091523810190f0138101cd640414f",
	                                                                         "_1f1091523810190f0138101cd641415b"));

	public static boolean isBlacklisted(String idRF) {
		return immeubles.contains(idRF);
	}
}
