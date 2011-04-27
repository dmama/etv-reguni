package ch.vd.uniregctb.migreg;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.TypeMigRegError;

public class MigrationErrorDAOImpl extends GenericDAOImpl<MigrationError, Long> implements MigrationErrorDAO {

	public MigrationErrorDAOImpl() {
		super(MigrationError.class);
	}

	public MigrationError getErrorForContribuable(long numeroCtb) {

		MigrationError error = null;

		Object[] criteria = {
				numeroCtb
			};
		String query = "from MigrationError m where m.noContribuable = ?";

		final List<?> list = find(query, criteria, FlushMode.MANUAL);
		if (list.size() > 0) {
			Assert.isEqual(1, list.size());
			error = (MigrationError) list.get(0);
		}

		return error;
	}

	public boolean existsForContribuable(final long numeroCtb) {
		return (Boolean) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(getPersistentClass());
				criteria.setProjection(Projections.rowCount());
				criteria.add(Restrictions.eq("noContribuable", numeroCtb));
				Integer count = (Integer) criteria.uniqueResult();
				return count.intValue() > 0;
			}
		});
	}

	public void removeForContribuable(final long numeroCtb) {
		getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				String hql = "delete from MIGREG_ERROR where NO_CONTRIBUABLE = " + numeroCtb;
				Query query = session.createSQLQuery(hql);
				query.executeUpdate();
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtb() {

		List<Long> list = getHibernateTemplate().find("select migreg_error.noContribuable from MigrationError as migreg_error");

		return list;
	}

	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtbForTypeError(final TypeMigRegError type) {
		return (List<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("select m.noContribuable from MigrationError as m where m.typeErreur = ?");
				query.setInteger(0, type.ordinal());
				return query.list();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtbForTypeErrorNeq(final TypeMigRegError type) {
		return (List<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("select m.noContribuable from MigrationError as m where m.typeErreur <> ?");
				query.setInteger(0, type.ordinal());
				return query.list();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<MigrationError> getMigregErrorsInCtbRange(int ctbStart, int ctbEnd) {
		List<MigrationError> list = null;
		if (ctbStart > 0 && ctbEnd > 0) {
			list = getHibernateTemplate().find("FROM MigrationError AS migreg WHERE migreg.noContribuable >= ? AND migreg.noContribuable <= ?", new Object[] { ctbStart, ctbEnd } );
		}
		else if (ctbStart > 0) {
			list = getHibernateTemplate().find("FROM MigrationError AS migreg WHERE migreg.noContribuable >= ?", ctbStart);
		}
		else if (ctbEnd > 0) {
			list = getHibernateTemplate().find("FROM MigrationError AS migreg WHERE migreg.noContribuable <= ?", ctbEnd);
		}
		else {
			Assert.isTrue(ctbStart < 0 && ctbEnd < 0);
			list = getHibernateTemplate().find("FROM MigrationError AS migreg");
		}
		return list;
	}
}
