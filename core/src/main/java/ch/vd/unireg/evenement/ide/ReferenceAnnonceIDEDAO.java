package ch.vd.uniregctb.evenement.ide;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public interface ReferenceAnnonceIDEDAO extends GenericDAO<ReferenceAnnonceIDE, Long> {

	/**
	 * @param etablissementId l'identifiant de l'établissement concerné
	 * @return une collection des événements attachés à l'établissement, dans l'ordre d'emmission croissant
	 */
	List<ReferenceAnnonceIDE> getReferencesAnnonceIDE(long etablissementId);

	/**
	 *
	 * @param etablissementId l'identifiant de l'établissement concerné
	 * @return le dernier événement émis pour un établissement
	 */
	ReferenceAnnonceIDE getLastReferenceAnnonceIDE(long etablissementId);

}
