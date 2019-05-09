package ch.vd.unireg.hibernate;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.HibernateQueryHelper;

public class HibernateTemplateImpl implements HibernateTemplate {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	@Override
	public <T> void delete(T entity) {
		getCurrentSession().delete(entity);
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
		return execute(null, callback);
	}

	@Override
	public <T> T execute(FlushModeType flushMode, HibernateCallback<T> callback) {
		final Session session = getCurrentSession();
		final FlushModeType old = session.getFlushMode();
		final boolean changeMode = (flushMode != null && old != flushMode);
		if (changeMode) {
			session.setFlushMode(flushMode);
		}
		try {
			return callback.doInHibernate(session);
		}
		catch (SQLException e) {
			throw new HibernateException(e);
		}
		finally {
			if (changeMode) {
				session.setFlushMode(old);
			}
		}
	}

	@Override
	public <T> T executeWithNewSession(HibernateCallback<T> callback) {
		final Session session = sessionFactory.openSession();
		try {
			return callback.doInHibernate(session);
		}
		catch (SQLException e) {
			throw new HibernateException(e);
		}
		finally {
			session.close();
		}
	}

	@Override
	public void flush() {
		getCurrentSession().flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> find(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushModeType flushMode) {
		final Session session = getCurrentSession();
		final FlushModeType oldFlushModeType = session.getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		try {
			final Query query = session.createQuery(hql);
			HibernateQueryHelper.assignNamedParameterValues(query, namedParams);
			return query.list();
		}
		finally {
			if (flushMode != null) {
				session.setFlushMode(oldFlushModeType);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T findUnique(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushModeType flushMode) {
		final Session session = getCurrentSession();
		final FlushModeType oldFlushModeType = session.getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		try {
			final Query query = session.createQuery(hql);
			HibernateQueryHelper.assignNamedParameterValues(query, namedParams);
			return (T) query.uniqueResult();
		}
		finally {
			if (flushMode != null) {
				session.setFlushMode(oldFlushModeType);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushModeType flushMode) {
		final Session session = getCurrentSession();
		final FlushModeType oldFlushModeType = session.getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		try {
			final Query query = session.createQuery(hql);
			HibernateQueryHelper.assignNamedParameterValues(query, namedParams);
			return query.iterate();
		}
		finally {
			if (flushMode != null) {
				session.setFlushMode(oldFlushModeType);
			}
		}
	}

	@Override
	public <T> List<T> find(String hql, @Nullable FlushModeType flushMode) {
		return find(hql, null, flushMode);
	}

	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable FlushModeType flushMode) {
		return iterate(hql, null, flushMode);
	}
}
