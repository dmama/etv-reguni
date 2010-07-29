package ch.vd.uniregctb.evenement;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;

/**
 *  Implementation du service expose pour gerer les evenements civils
 * @author xcifde
 *
 */
public class EvenementServiceImpl implements EvenementService{

	private EvenementCivilDAO evenementCivilDAO;
	
	public void setEvenementCivilDAO(EvenementCivilDAO evenementCivilDAO) {
		this.evenementCivilDAO = evenementCivilDAO;
	}


	/**
	 * @see ch.vd.uniregctb.evenement.EvenementService#find(ch.vd.uniregctb.evenement.EvenementCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	public List<EvenementCivilData> find(EvenementCriteria criterion, ParamPagination paramPagination) {
		return evenementCivilDAO.find(criterion, paramPagination);
	}

	/**
	 * @see ch.vd.uniregctb.evenement.EvenementService#count(ch.vd.uniregctb.evenement.EvenementCriteria)
	 */
	public int count(EvenementCriteria criterion){
		return evenementCivilDAO.count(criterion);
	}
	/**
	 * @see ch.vd.uniregctb.evenement.EvenementService#get(java.lang.Long)
	 */
	public EvenementCivilData get(Long id) {
		return evenementCivilDAO.get(id);
	}

}
