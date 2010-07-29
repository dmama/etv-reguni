package ch.vd.uniregctb.audit;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.uniregctb.common.ParamPagination;

public interface AuditLineDAO extends GenericDAO<AuditLine, Long> {

	public static final int DEFAULT_BATCH_SIZE = 50;

	public List<AuditLine> find(AuditLineCriteria criterion, ParamPagination paramPagination);

	public List<AuditLine> findLastCountFromID(long id, int count);

	public List<AuditLine> find(long evenementCivilId);

	public void insertLineInNewTx(AuditLine line);

	public int count(AuditLineCriteria criteria);
}
