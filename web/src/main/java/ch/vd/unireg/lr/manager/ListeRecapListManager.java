package ch.vd.uniregctb.lr.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.declaration.ListeRecapitulativeCriteria;
import ch.vd.uniregctb.lr.view.ListeRecapitulativeSearchResult;

/**
 * Definition de services utiles pour la recherche de LR
 *
 * @author xcifde
 *
 */
public interface ListeRecapListManager {

	/**
	 * Recherche de listes recapitulatives suivant certains criteres
	 *
	 * @param lrCriteria
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	List<ListeRecapitulativeSearchResult> find(ListeRecapitulativeCriteria lrCriteria, ParamPagination paramPagination) throws AdresseException;

	/**
	 * Renvoie le nombre de listes récapitulatives correspondant aux critères donnés
	 * @param lrCriteria
	 * @return
	 */
	@Transactional(readOnly = true)
	int count(ListeRecapitulativeCriteria lrCriteria);

}
