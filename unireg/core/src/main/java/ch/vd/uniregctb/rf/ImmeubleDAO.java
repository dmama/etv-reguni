package ch.vd.uniregctb.rf;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface ImmeubleDAO extends GenericDAO<Immeuble, Long> {

	/**
	 * Recherche le nombre d'immeubles en possession du contribuable spécifié.
	 *
	 * @param proprietaireId le numéro de contribuable du propriétaire de l'immeuble
	 * @return le nombre d'immeuble appartenant au contribuable.
	 */
	int count(long proprietaireId);

	/**
	 * Recherche tous les immeubles en possession du contribuable spécifié.
	 *
	 * @param proprietaireId le numéro de contribuable du propriétaire de l'immeuble
	 * @return la liste des immeuble appartenant au contribuable (la liste peut être vide).
	 */
	List<Immeuble> find(long proprietaireId);
}
