package ch.vd.uniregctb.audit;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class AuditLineDAOImpl extends BaseDAOImpl<AuditLine, Long> implements AuditLineDAO, InitializingBean {

	private Dialect dialect;
	private PlatformTransactionManager transactionManager;
	private String nextValSql;

	public AuditLineDAOImpl() {
		super(AuditLine.class);
	}

	@Override
	public List<AuditLine> find(final AuditLineCriteria criterion, final ParamPagination paramPagination) {
		final Session session = getCurrentSession();
		final QueryFragment fragment = new QueryFragment(buildSql(criterion));
		fragment.add(paramPagination.buildOrderClause("a", null, false, null));

		final Query queryObject = fragment.createQuery(session);
		final int firstResult = paramPagination.getSqlFirstResult();
		final int maxResult = paramPagination.getSqlMaxResults();
		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);

		//noinspection unchecked
		return queryObject.list();
	}

	private static String buildSql(AuditLineCriteria criterion) {
		String query = "from AuditLine a where 1=1";
		if (!criterion.isShowError()) {
			query += " AND a.level != 'ERROR'";
		}
		if (!criterion.isShowInfo()) {
			query += " AND a.level != 'INFO'";
		}
		if (!criterion.isShowSuccess()) {
			query += " AND a.level != 'SUCCESS'";
		}
		if (!criterion.isShowWarning()) {
			query += " AND a.level != 'WARN'";
		}
		if (!criterion.isShowEvCivil()) {
			query += " AND a.evenementId is null";
		}
		return query;
	}

	@Override
	public int count(AuditLineCriteria criteria) {
		final String query = "select count(*) " + buildSql(criteria);
		return DataAccessUtils.intResult(find(query, null));
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<AuditLine> findLastCountFromID(final long id, final int count) {
		if (count == 0) {
			return Collections.emptyList();
		}
		final String query = "FROM AuditLine line WHERE line.id >= :start ORDER BY line.id DESC";
		final Session session = getCurrentSession();
		final Query queryObject = session.createQuery(query);
		queryObject.setParameter("start", id);
		queryObject.setMaxResults(count);
		return queryObject.list();
	}

	@Override
	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	public List<AuditLine> find(final long evenementCivilId) {
		final String query = "FROM AuditLine line WHERE line.evenementId = :id ORDER BY line.id ASC";
		final Session session = getCurrentSession();
		final Query queryObject = session.createQuery(query);
		queryObject.setParameter("id", evenementCivilId);
		return queryObject.list();
	}

	/**
	 *
	 * @see ch.vd.uniregctb.audit.AuditLineDAO#insertLineInNewTx(ch.vd.uniregctb.audit.AuditLine)
	 */
	@Override
	public void insertLineInNewTx(final AuditLine line) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Timestamp now = new Timestamp(DateHelper.getCurrentDate().getTime());
				final long id = getNextId();
				final Query query = getCurrentSession().createSQLQuery("insert into AUDIT_LOG (id, LOG_LEVEL, DOC_ID, EVT_ID, THREAD_ID, MESSAGE, LOG_DATE, LOG_USER) values (?, ?, ?, ?, ?, ?, ?, ?)");
				query.setLong(0, id);
				query.setParameter(1, line.getLevel().toString());
				query.setParameter(2, line.getDocumentId(), StandardBasicTypes.LONG);
				query.setParameter(3, line.getEvenementId(), StandardBasicTypes.LONG);
				query.setParameter(4, line.getThreadId(), StandardBasicTypes.LONG);
				query.setParameter(5, line.getMessage());
				query.setTimestamp(6, now);
				query.setParameter(7, AuthenticationHelper.getCurrentPrincipal());
				query.executeUpdate();
				return null;
			}
		});
	}

	private long getNextId() {
		final Query query = getCurrentSession().createSQLQuery(nextValSql);
		return ((Number) query.uniqueResult()).longValue();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		nextValSql = dialect.getSequenceNextValString("hibernate_sequence");
	}

	@Override
	public int purge(RegDate seuilPurge) {
		final Timestamp seuilTimestamp = new Timestamp(seuilPurge.asJavaDate().getTime());
		final Query query = getCurrentSession().createSQLQuery("delete from AUDIT_LOG WHERE LOG_DATE < :seuil");
		query.setTimestamp("seuil", seuilTimestamp);
		return query.executeUpdate();
	}
}
