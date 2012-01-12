package ch.vd.uniregctb.evenement.civil.externe;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;

/**
 * Service expose pour gerer les evenements civils
 * @author xcifde
 *
 */
public interface EvenementService {

	/**
	 * Cherche les evenements correspondant aux criteres
	 * @param criterion
	 * @return
	 */
	public List<EvenementCivilExterne> find(EvenementCivilExterneCriteria criterion, ParamPagination paramPagination);

	/**
	 * Cherche et compte les evenements correspondant aux criteres
	 * @param criterion
	 * @return
	 */
	public int count(EvenementCivilExterneCriteria criterion);
	
	/**
	 * Charge l'evenement de l'id correspondant
	 * @param id
	 * @return
	 */
	public EvenementCivilExterne get(Long id);
	
}
