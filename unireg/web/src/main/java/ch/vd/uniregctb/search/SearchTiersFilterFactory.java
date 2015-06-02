package ch.vd.uniregctb.search;

public interface SearchTiersFilterFactory {

	/**
	 * Parse les paramètres du filtre et retourne un filtre sous forme d'objet.
	 *
	 * @param params les paramètres sous forme de string
	 * @return un filtre
	 */
	SearchTiersFilter parse(String params);
}
