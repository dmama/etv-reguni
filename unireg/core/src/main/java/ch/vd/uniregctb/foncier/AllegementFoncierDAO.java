package ch.vd.uniregctb.foncier;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface AllegementFoncierDAO extends GenericDAO<AllegementFoncier, Long> {

	/**
	 * @param idContribuable numéro de contribuable
	 * @param idImmeuble identifiant d'un immeuble RF
	 * @param clazz class des allègements fonciers souhaités
	 * @param <T> type de la classe des allègements fonciers souhaités
	 * @return la liste des allègements fonciers de la classe désirée existants entre le contribuable donné et l'immeuble donné
	 */
	<T extends AllegementFoncier> List<T> getAllegementsFonciers(long idContribuable, long idImmeuble, Class<T> clazz);
}
