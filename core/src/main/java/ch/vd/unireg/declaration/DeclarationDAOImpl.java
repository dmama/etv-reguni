package ch.vd.unireg.declaration;

import javax.persistence.FlushModeType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ch.vd.unireg.common.BaseDAOImpl;

public abstract class DeclarationDAOImpl<T extends Declaration> extends BaseDAOImpl<T, Long> implements DeclarationDAO<T> {

	protected DeclarationDAOImpl(Class<T> persistentClass) {
		super(persistentClass);
	}

	@Override
	public final <U extends T> Set<U> getDeclarationsAvecDelaisEtEtats(Class<U> clazz, Collection<Long> ids) {
		final Session session = getCurrentSession();
		final Criteria crit = session.createCriteria(clazz);
		crit.add(Restrictions.in("id", ids));
		crit.setFetchMode("etats", FetchMode.JOIN);
		crit.setFetchMode("delais", FetchMode.JOIN);
		crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		final FlushModeType mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushModeType.COMMIT);
			//noinspection unchecked
			return new HashSet<>(crit.list());
		}
		finally {
			session.setFlushMode(mode);
		}
	}

	@Override
	public final Set<T> getDeclarationsAvecDelaisEtEtats(Collection<Long> ids) {
		return getDeclarationsAvecDelaisEtEtats(getPersistentClass(), ids);
	}
}
