package ch.vd.unireg.migreg;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.type.TypeMigRegError;

public class MigrationErrorDAOImpl extends BaseDAOImpl<MigrationError, Long> implements MigrationErrorDAO {

	public MigrationErrorDAOImpl() {
		super(MigrationError.class);
	}

	@Override
	public MigrationError getErrorForContribuable(long numeroCtb) {

		MigrationError error = null;
		final String query = "from MigrationError m where m.noContribuable = :noCtb";
		final List<MigrationError> list = find(query, buildNamedParameters(Pair.of("noCtb", numeroCtb)), FlushMode.MANUAL);
		if (!list.isEmpty()) {
			Assert.isEqual(1, list.size());
			error = list.get(0);
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
		return find("select migreg_error.noContribuable from MigrationError as migreg_error", null);
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
			list = find("FROM MigrationError AS migreg WHERE migreg.noContribuable >= :min AND migreg.noContribuable <= :max",
			            buildNamedParameters(Pair.of("min", ctbStart), Pair.of("max", ctbEnd)),
			            null);
		}
		else if (ctbStart > 0) {
			list = find("FROM MigrationError AS migreg WHERE migreg.noContribuable >= :min",
			            buildNamedParameters(Pair.of("min", ctbStart)),
			            null);
		}
		else if (ctbEnd > 0) {
			list = find("FROM MigrationError AS migreg WHERE migreg.noContribuable <= :max",
			            buildNamedParameters(Pair.of("max", ctbEnd)),
			            null);
		}
		else {
			Assert.isTrue(ctbStart < 0 && ctbEnd < 0);
			list = find("FROM MigrationError AS migreg", null);
		}
		return list;
	}
}