package ch.vd.uniregctb.hibernate;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

public class HibernateTemplateImpl implements HibernateTemplate {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T merge(T entity) {
		return (T) getCurrentSession().merge(entity);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> clazz, Serializable id) {
		return (T) getCurrentSession().get(clazz, id);
	}

	@Override
	public <T> T execute(HibernateCallback<T> callback) {
		try {
			return callback.doInHibernate(getCurrentSession());
		}
		catch (SQLException e) {
			throw new HibernateException(e);
		}
	}

	@Override
	public void flush() {
		getCurrentSession().flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> find(String hql, @Nullable Object[] params, @Nullable FlushMode flushMode) {
		final Session session = getCurrentSession();
		final FlushMode oldFlushMode = session.getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		try {
			final Query query = session.createQuery(hql);
			if (params != null && params.length > 0) {
				for (int i = 0 ; i < params.length ; ++ i) {
					query.setParameter(i, params[i]);
				}
			}
			return query.list();
		}
		finally {
			if (flushMode != null) {
				session.setFlushMode(oldFlushMode);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable Object[] params, @Nullable FlushMode flushMode) {
		final Session session = getCurrentSession();
		final FlushMode oldFlushMode = session.getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		try {
			final Query query = session.createQuery(hql);
			if (params != null && params.length > 0) {
				for (int i = 0 ; i < params.length ; ++ i) {
					query.setParameter(i, params[i]);
				}
			}
			return query.iterate();
		}
		finally {
			if (flushMode != null) {
				session.setFlushMode(oldFlushMode);
			}
		}
	}
}
