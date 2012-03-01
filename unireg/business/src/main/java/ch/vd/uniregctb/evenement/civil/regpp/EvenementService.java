package ch.vd.uniregctb.evenement.civil.regpp;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;

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
	public List<EvenementCivilRegPP> find(EvenementCivilCriteria criterion, ParamPagination paramPagination);

	/**
	 * Cherche et compte les evenements correspondant aux criteres
	 * @param criterion
	 * @return
	 */
	public int count(EvenementCivilCriteria criterion);
	
	/**
	 * Charge l'evenement de l'id correspondant
	 * @param id
	 * @return
	 */
	public EvenementCivilRegPP get(Long id);
	
}
