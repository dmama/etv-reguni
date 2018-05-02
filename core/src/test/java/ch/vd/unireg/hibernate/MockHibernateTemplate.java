package ch.vd.unireg.hibernate;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockHibernateTemplate implements HibernateTemplate {
	@Override
	public <T> void delete(T entity) {
		throw new NotImplementedException();
	}

	@Override
	public <T> T merge(T entity) {
		throw new NotImplementedException();
	}

	@Override
	public <T> T get(Class<T> clazz, Serializable id) {
		throw new NotImplementedException();
	}

	@Override
	public <T> T execute(HibernateCallback<T> callback) throws HibernateException {
		throw new NotImplementedException();
	}

	@Override
	public <T> T execute(FlushMode flushMode, HibernateCallback<T> callback) throws HibernateException {
		throw new NotImplementedException();
	}

	@Override
	public <T> T executeWithNewSession(HibernateCallback<T> callback) throws HibernateException {
		throw new NotImplementedException();
	}

	@Override
	public void flush() {

	}

	@Override
	public <T> List<T> find(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushMode) {
		throw new NotImplementedException();
	}

	@Override
	public <T> T findUnique(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushMode) {
		throw new NotImplementedException();
	}

	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushMode) {
		throw new NotImplementedException();
	}

	@Override
	public <T> List<T> find(String hql, @Nullable FlushMode flushMode) {
		throw new NotImplementedException();
	}

	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable FlushMode flushMode) {
		throw new NotImplementedException();
	}
}
