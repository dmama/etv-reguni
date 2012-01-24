package ch.vd.uniregctb.evenement.civil.regpp;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;

/**
 *  Implementation du service expose pour gerer les evenements civils
 * @author xcifde
 *
 */
public class EvenementServiceImpl implements EvenementService{

	private EvenementCivilRegPPDAO evenementCivilExterneDAO;
	
	public void setEvenementCivilExterneDAO(EvenementCivilRegPPDAO evenementCivilExterneDAO) {
		this.evenementCivilExterneDAO = evenementCivilExterneDAO;
	}


	/**
	 * @see EvenementService#find(EvenementCivilRegPPCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	@Override
	public List<EvenementCivilRegPP> find(EvenementCivilRegPPCriteria criterion, ParamPagination paramPagination) {
		return evenementCivilExterneDAO.find(criterion, paramPagination);
	}

	/**
	 * @see EvenementService#count(EvenementCivilRegPPCriteria)
	 */
	@Override
	public int count(EvenementCivilRegPPCriteria criterion){
		return evenementCivilExterneDAO.count(criterion);
	}
	/**
	 * @see EvenementService#get(java.lang.Long)
	 */
	@Override
	public EvenementCivilRegPP get(Long id) {
		return evenementCivilExterneDAO.get(id);
	}

}
