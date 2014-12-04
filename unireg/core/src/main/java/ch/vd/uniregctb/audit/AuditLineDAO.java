package ch.vd.uniregctb.audit;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;

public interface AuditLineDAO extends GenericDAO<AuditLine, Long> {

	List<AuditLine> find(AuditLineCriteria criterion, ParamPagination paramPagination);

	List<AuditLine> findLastCountFromID(long id, int count);

	List<AuditLine> find(long evenementCivilId);

	void insertLineInNewTx(AuditLine line);

	int count(AuditLineCriteria criteria);

	/**
	 * @param seuilPurge date avant laquelle des lignes d'audit doivent être effacées
	 * @return nombre de lignes d'audit effectivement effacées
	 */
	int purge(RegDate seuilPurge);
}
