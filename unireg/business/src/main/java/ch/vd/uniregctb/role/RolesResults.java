package ch.vd.uniregctb.role;

import java.util.Map;

/**
 * Interface d'interrogation des résultats contenu dans une entité {@link Roles}
 * @param <ICOM> Type d'information de commune
 */
public interface RolesResults<ICOM extends InfoCommune<?, ICOM>> {

	/**
	 * @return les informations récoltées sur les communes
	 */
	Map<Integer, ICOM> getInfosCommunes();
}
