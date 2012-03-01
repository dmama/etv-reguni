package ch.vd.uniregctb.evenement.civil.regpp;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;

/**
 *  Implementation du service expose pour gerer les evenements civils
 * @author xcifde
 *
 */
public class EvenementServiceImpl implements EvenementService{

	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	
	public void setEvenementCivilRegPPDAO(EvenementCivilRegPPDAO evenementCivilRegPPDAO) {
		this.evenementCivilRegPPDAO = evenementCivilRegPPDAO;
	}


	/**
	 * @see EvenementService#find(ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	@Override
	public List<EvenementCivilRegPP> find(EvenementCivilCriteria criterion, ParamPagination paramPagination) {
		return evenementCivilRegPPDAO.find(criterion, paramPagination);
	}

	/**
	 * @see EvenementService#count(ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria)
	 */
	@Override
	public int count(EvenementCivilCriteria criterion){
		return evenementCivilRegPPDAO.count(criterion);
	}
	/**
	 * @see EvenementService#get(java.lang.Long)
	 */
	@Override
	public EvenementCivilRegPP get(Long id) {
		return evenementCivilRegPPDAO.get(id);
	}

}
