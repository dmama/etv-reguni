package ch.vd.unireg.migreg;

import javax.persistence.FlushModeType;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;

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
		final List<MigrationError> list = find(query, buildNamedParameters(Pair.of("noCtb", numeroCtb)), FlushModeType.COMMIT);
		if (!list.isEmpty()) {
			if (list.size() != 1) {
				throw new IllegalArgumentException();
			}
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
		final Query query = session.createNativeQuery(hql);
		query.executeUpdate();
	}

	@Override
	public List<Long> getAllNoCtb() {
		return find("select migreg_error.noContribuable from MigrationError as migreg_error", null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtbForTypeError(final TypeMigRegError type) {
		final Session session = getCurrentSession();
		final Query query = session.createQuery("select m.noContribuable from MigrationError as m where m.typeErreur = ?");
		query.setParameter(0, type.ordinal());
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllNoCtbForTypeErrorNeq(final TypeMigRegError type) {
		final Session session = getCurrentSession();
		Query query = session.createQuery("select m.noContribuable from MigrationError as m where m.typeErreur <> ?");
		query.setParameter(0, type.ordinal());
		return query.list();
	}

	@Override
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
			list = find("FROM MigrationError AS migreg", null);
		}
		return list;
	}
}
