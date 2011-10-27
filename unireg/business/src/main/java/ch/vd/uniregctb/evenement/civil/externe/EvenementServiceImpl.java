package ch.vd.uniregctb.evenement.civil.externe;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;

/**
 *  Implementation du service expose pour gerer les evenements civils
 * @author xcifde
 *
 */
public class EvenementServiceImpl implements EvenementService{

	private EvenementCivilExterneDAO evenementCivilExterneDAO;
	
	public void setEvenementCivilExterneDAO(EvenementCivilExterneDAO evenementCivilExterneDAO) {
		this.evenementCivilExterneDAO = evenementCivilExterneDAO;
	}


	/**
	 * @see EvenementService#find(EvenementCivilExterneCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	@Override
	public List<EvenementCivilExterne> find(EvenementCivilExterneCriteria criterion, ParamPagination paramPagination) {
		return evenementCivilExterneDAO.find(criterion, paramPagination);
	}

	/**
	 * @see EvenementService#count(EvenementCivilExterneCriteria)
	 */
	@Override
	public int count(EvenementCivilExterneCriteria criterion){
		return evenementCivilExterneDAO.count(criterion);
	}
	/**
	 * @see EvenementService#get(java.lang.Long)
	 */
	@Override
	public EvenementCivilExterne get(Long id) {
		return evenementCivilExterneDAO.get(id);
	}

}
