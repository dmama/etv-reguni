package ch.vd.uniregctb.etiquette;

import java.util.List;

/**
 * Service de gestion des étiquettes et de leur assignation temporelles à des tiers
 */
public interface EtiquetteService {

	/**
	 * @return la liste de toutes les étiquettes existantes
	 */
	List<Etiquette> getAllEtiquettes();

	/**
	 * @param doNotAutoflush <code>true</code> s'il ne faut pas laisser la session subir un autoflush pendant la requête de récupération des données en base
	 * @return la liste de toutes les étiquettes existantes
	 */
	List<Etiquette> getAllEtiquettes(boolean doNotAutoflush);

	/**
	 * @param id identifiant de l'étiquette
	 * @return l'étiquette correspondant à l'identifiant, ou <code>null</code> s'il n'y en a pas
	 */
	Etiquette getEtiquette(long id);

	/**
	 * @param code le code de l'étiquette
	 * @return l'étiquette correspondant au code, ou <code>null</code> s'il n'y en a pas
	 */
	Etiquette getEtiquette(String code);

	/**
	 * @param id l'identifiant de l'étiquette de tiers
	 * @return l'étiquette de tiers correspondant à l'identifiant, ou <code>null</code> s'il n'y en a pas
	 */
	EtiquetteTiers getEtiquetteTiers(long id);

}
