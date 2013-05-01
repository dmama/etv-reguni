package ch.vd.uniregctb.migreg;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.TypeMigRegError;

public class MigrationErrorDAOImpl extends GenericDAOImpl<MigrationError, Long> implements MigrationErrorDAO {

	public MigrationErrorDAOImpl() {
		super(MigrationError.class);
	}

	@Override
	public MigrationError getErrorForContribuable(long numeroCtb) {

		MigrationError error = null;

		Object[] criteria = {
				numeroCtb
			};
		String query = "from MigrationError m where m.noContribuable = ?";

		final List<?> list = find(query, criteria, FlushMode.MANUAL);
		if (!list.isEmpty()) {
			Assert.isEqual(1, list.size());
			error = (MigrationError) list.get(0);
		}

		return error;
	}

	@Override
	public boolean existsForContribuable(final long numeroCtb) {
		final Session session = getCurrentSession();
		final Criteria criteria = session.createCriteria(getPersistentClass());
		criteria.setProjection(Projections.rowCount());
		criteria.add(Restrictions.eq("noContribuable", numeroCtb));
		final int count = ((Number) criteria.uniqueResult()).intValue();
		return count > 0;
	}

	@Override
	public void removeForContribuable(final long numeroCtb) {
		final Session session = getCurrentSession();
		final String hql = "delete from MIGREG_ERROR where NO_CONTRIBUABLE = " + numeroCtb;
		final Query query = session.createSQLQuery(hql);
		query.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtb() {
		return (List<Long>) find("select migreg_error.noContribuable from MigrationError as migreg_error", null, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtbForTypeError(final TypeMigRegError type) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("select m.noContribuable from MigrationError as m where m.typeErreur = ?");
		query.setInteger(0, type.ordinal());
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtbForTypeErrorNeq(final TypeMigRegError type) {
		final Session session = getCurrentSession();
		Query query = session.createQuery("select m.noContribuable from MigrationError as m where m.typeErreur <> ?");
		query.setInteger(0, type.ordinal());
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<MigrationError> getMigregErrorsInCtbRange(int ctbStart, int ctbEnd) {
		final List<MigrationError> list;
		if (ctbStart > 0 && ctbEnd > 0) {
			list = (List<MigrationError>) find("FROM MigrationError AS migreg WHERE migreg.noContribuable >= ? AND migreg.noContribuable <= ?", new Object[]{ctbStart, ctbEnd}, null);
		}
		else if (ctbStart > 0) {
			list = (List<MigrationError>) find("FROM MigrationError AS migreg WHERE migreg.noContribuable >= ?", new Object[]{ctbStart}, null);
		}
		else if (ctbEnd > 0) {
			list = (List<MigrationError>) find("FROM MigrationError AS migreg WHERE migreg.noContribuable <= ?", new Object[] {ctbEnd}, null);
		}
		else {
			Assert.isTrue(ctbStart < 0 && ctbEnd < 0);
			list = (List<MigrationError>) find("FROM MigrationError AS migreg", null, null);
		}
		return list;
	}
}
