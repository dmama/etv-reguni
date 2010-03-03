package ch.vd.uniregctb.interfaces.model;

import java.util.List;

public interface Commune extends CommuneSimple {

	/**
	 * Retourne la collectivité en charge de l'administration de la commune.
	 *
	 * @return la collectivité en charge de l'administration de la commune.
	 */
	CollectiviteAdministrative getAdminstreePar();

	/**
	 * Retourne la liste des collectivités administratives de la commune. Cette liste contient des objets de type
	 * {@link CollectiviteAdministrative}.
	 *
	 * @return la liste des collectivités administratives de la commune.
	 */
	List<CollectiviteAdministrative> getCollectivites();

}
