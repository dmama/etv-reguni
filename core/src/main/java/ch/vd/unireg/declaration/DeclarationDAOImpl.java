package ch.vd.unireg.declaration;

import javax.persistence.FlushModeType;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;

import ch.vd.unireg.common.BaseDAOImpl;

public abstract class DeclarationDAOImpl<T extends Declaration> extends BaseDAOImpl<T, Long> implements DeclarationDAO<T> {

	protected DeclarationDAOImpl(Class<T> persistentClass) {
		super(persistentClass);
	}

	@Override
	public final <U extends T> Set<U> getDeclarationsAvecDelaisEtEtats(Class<U> clazz, Collection<Long> ids) {
		final Session session = getCurrentSession();
		final FlushModeType mode = session.getFlushMode();
		try {
			session.setFlushMode(FlushModeType.COMMIT);
			final CriteriaQuery<U> query = session.getCriteriaBuilder().createQuery(clazz);
			final Root<U> root = query.from(clazz);
			root.fetch("etats", JoinType.LEFT); // force le préchargement
			root.fetch("delais", JoinType.LEFT); // force le préchargement
			query.distinct(true);
			query.where(root.get("id").in(ids));
			return new HashSet<>(session.createQuery(query).list());
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
