package ch.vd.uniregctb.tiers.picker;

public interface TiersPickerFilterFactory {

	/**
	 * Parse les paramètres du filtre et retourne un filtre sous forme d'objet.
	 *
	 * @param params les paramètres sous forme de string
	 * @return un filtre
	 */
	TiersPickerFilter parse(String params);
}
