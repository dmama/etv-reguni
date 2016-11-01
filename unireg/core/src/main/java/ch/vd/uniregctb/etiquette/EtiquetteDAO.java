package ch.vd.uniregctb.etiquette;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * DAO qui traite des étiquettes
 */
public interface EtiquetteDAO extends GenericDAO<Etiquette, Long> {

	/**
	 * @param code un code d'étiquette
	 * @return l'étiquette correspondante
	 */
	Etiquette getByCode(String code);
}
