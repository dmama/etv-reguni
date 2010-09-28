package ch.vd.uniregctb.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hsqldb.Types;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ParamPagination;

public class AuditLineDAOImpl extends GenericDAOImpl<AuditLine, Long> implements AuditLineDAO, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(AuditLineDAOImpl.class);

	private Dialect dialect;
	private DataSource dataSource;
	private String nextValSql;

	public AuditLineDAOImpl() {
		super(AuditLine.class);
	}

	@SuppressWarnings("unchecked")
	public List<AuditLine> find(final AuditLineCriteria criterion, final ParamPagination paramPagination) {

		return (List<AuditLine>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				String query = buildSql(criterion);
				query = query + buildOrderClause(paramPagination);
				Query queryObject = session.createQuery(query);

				int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
				int maxResult = paramPagination.getTaillePage();
				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				return queryObject.list();
			}
		});
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

	private static String buildOrderClause(ParamPagination paramPagination) {
		String clauseOrder = "";
		if (paramPagination.getChamp() != null) {
			if (paramPagination.getChamp().equals("type")) {
				clauseOrder = " order by a.class";
			}
			else {
				clauseOrder = " order by a." + paramPagination.getChamp();
			}

			if (paramPagination.isSensAscending()) {
				clauseOrder = clauseOrder + " asc";
			}
			else {
				clauseOrder = clauseOrder + " desc";
			}
		}
		else {
			clauseOrder = " order by a.id desc";

		}
		return clauseOrder;
	}

	public int count(AuditLineCriteria criteria) {
		final String query = "select count(*) " + buildSql(criteria);
		return DataAccessUtils.intResult(getHibernateTemplate().find(query));
	}

	@SuppressWarnings("unchecked")
	public List<AuditLine> findLastCountFromID(final long id, final int count) {

		final String query = "FROM AuditLine line WHERE line.id >= :start ORDER BY line.id DESC";

		final List<AuditLine> list = (List<AuditLine>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				final Query queryObject = session.createQuery(query);
				queryObject.setParameter("start", id);
				queryObject.setMaxResults(count);
				return queryObject.list();
			}
		});

		return list;
	}

	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	public List<AuditLine> find(final long evenementCivilId) {

		final String query = "FROM AuditLine line WHERE line.evenementId = :id ORDER BY line.id ASC";

		final List<AuditLine> list = (List<AuditLine>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				final Query queryObject = session.createQuery(query);
				queryObject.setParameter("id", evenementCivilId);
				return queryObject.list();
			}
		});

		return list;
	}

	/**
	 *
	 * @see ch.vd.uniregctb.audit.AuditLineDAO#insertLineInNewTx(ch.vd.uniregctb.audit.AuditLine)
	 */
	public void insertLineInNewTx(final AuditLine line) {

		doWithNewConnection(new Callback<Object>() {
			public Object execute(Connection connection) throws SQLException {
				final long id = getNextId(connection);
				final Timestamp now = new Timestamp(DateHelper.getCurrentDate().getTime());

				final PreparedStatement stat = connection.prepareStatement("insert into AUDIT_LOG (id, LOG_LEVEL, DOC_ID, EVT_ID, THREAD_ID, MESSAGE, LOG_DATE, LOG_USER) values (?, ?, ?, ?, ?, ?, ?, ?)");
				stat.setLong(1, id);
				stat.setString(2, line.getLevel().toString());
				stat.setObject(3, line.getDocumentId(), Types.BIGINT);
				stat.setObject(4, line.getEvenementId(), Types.BIGINT);
				stat.setObject(5, line.getThreadId(), Types.BIGINT);
				stat.setObject(6, line.getMessage(), Types.VARCHAR);
				stat.setTimestamp(7, now);
				stat.setObject(8, AuthenticationHelper.getCurrentPrincipal(), Types.VARCHAR);

				stat.execute();
				return null;
			}
		});
	}

	private static interface Callback<T> {
		T execute(Connection connection) throws SQLException;
	}

	private <T> T doWithNewConnection(Callback<T> callback) {
		try {
			final Connection con = dataSource.getConnection();
			try {
				return callback.execute(con);
			}
			finally {
				con.close();
			}
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (SQLException e) {
			LOGGER.error(e, e);
			throw new RuntimeException(e);
		}
	}

	private long getNextId(Connection con) throws SQLException {
		final Statement stat = con.createStatement();
		final ResultSet rs = stat.executeQuery(nextValSql);
		Assert.isTrue(rs.next());
		return rs.getLong(1);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	protected void initDao() throws Exception {
		super.initDao();
		nextValSql = dialect.getSequenceNextValString("hibernate_sequence");
	}

	public int purge(final int delaiPurge) {
		Assert.isTrue(delaiPurge > 0);
		return doWithNewConnection(new Callback<Integer>() {
			public Integer execute(Connection connection) throws SQLException {
				final RegDate seuilPurge = RegDate.get().addDays(- delaiPurge);
				final Timestamp seuilTimestamp = new Timestamp(seuilPurge.asJavaDate().getTime());
				final PreparedStatement stat = connection.prepareStatement("delete from AUDIT_LOG WHERE LOG_DATE < ?");
				stat.setTimestamp(1, seuilTimestamp);
				stat.execute();
				return stat.getUpdateCount();
			}
		});
	}
}
