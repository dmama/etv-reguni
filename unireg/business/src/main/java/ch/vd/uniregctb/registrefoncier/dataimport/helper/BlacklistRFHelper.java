package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Collections;
import java.util.Set;

/**
 * Contient les identifiants des éléments à ne pas imnporter du registe foncier pour une raison ou une autre.
 */
public class BlacklistRFHelper {

	// [SIFISC-22366] On ignore l'unique immeuble sur la commune 'Autre Registre foncier'.
	private static final Set<String> immeubles = Collections.singleton("_1f1091523810108101381012b3d64cb4");

	public static boolean isBlacklisted(String idRF) {
		return immeubles.contains(idRF);
	}
}
