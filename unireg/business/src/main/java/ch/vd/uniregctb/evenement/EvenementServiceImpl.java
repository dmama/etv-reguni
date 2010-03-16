package ch.vd.uniregctb.evenement;

import java.util.List;

import ch.vd.uniregctb.common.ParamPagination;

/**
 *  Implementation du service expose pour gerer les evenements civils
 * @author xcifde
 *
 */
public class EvenementServiceImpl implements EvenementService{

	private EvenementCivilRegroupeDAO evenementCivilRegroupeDAO;
	
	public EvenementCivilRegroupeDAO getEvenementCivilRegroupeDAO() {
		return evenementCivilRegroupeDAO;
	}

	public void setEvenementCivilRegroupeDAO(EvenementCivilRegroupeDAO evenementCivilRegroupeDAO) {
		this.evenementCivilRegroupeDAO = evenementCivilRegroupeDAO;
	}


	/**
	 * @see ch.vd.uniregctb.evenement.EvenementService#find(ch.vd.uniregctb.evenement.EvenementCriteria, ch.vd.uniregctb.common.ParamPagination)
	 */
	public List<EvenementCivilRegroupe> find(EvenementCriteria criterion, ParamPagination paramPagination) {
		return evenementCivilRegroupeDAO.find(criterion, paramPagination);
	}

	/**
	 * @see ch.vd.uniregctb.evenement.EvenementService#count(ch.vd.uniregctb.evenement.EvenementCriteria)
	 */
	public int count(EvenementCriteria criterion){
		return evenementCivilRegroupeDAO.count(criterion);
	}
	/**
	 * @see ch.vd.uniregctb.evenement.EvenementService#get(java.lang.Long)
	 */
	public EvenementCivilRegroupe get(Long id) {
		return evenementCivilRegroupeDAO.get(id);
	}

}
