package ch.vd.unireg.etiquette;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * DAO qui traite des étiquettes
 */
public interface EtiquetteDAO extends GenericDAO<Etiquette, Long> {

	/**
	 * @param doNotAutoflush <code>true</code> s'il ne faut pas laisser la session subir un autoflush pendant la requête de récupération des données en base
	 * @return la liste de toutes les étiquettes existantes
	 */
	List<Etiquette> getAll(boolean doNotAutoflush);

	/**
	 * @param code un code d'étiquette
	 * @return l'étiquette correspondante
	 */
	Etiquette getByCode(String code);
}
