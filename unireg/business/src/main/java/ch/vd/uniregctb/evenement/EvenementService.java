package ch.vd.uniregctb.evenement;

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
	public List<EvenementCivilData> find(EvenementCriteria criterion, ParamPagination paramPagination);

	/**
	 * Cherche et compte les evenements correspondant aux criteres
	 * @param criterion
	 * @return
	 */
	public int count(EvenementCriteria criterion);
	
	/**
	 * Charge l'evenement de l'id correspondant
	 * @param id
	 * @return
	 */
	public EvenementCivilData get(Long id);
	
}
