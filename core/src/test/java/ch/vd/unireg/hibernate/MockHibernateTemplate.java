package ch.vd.unireg.hibernate;

import javax.persistence.FlushModeType;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.HibernateException;
import org.jetbrains.annotations.Nullable;

public class MockHibernateTemplate implements HibernateTemplate {
	@Override
	public <T> void delete(T entity) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> T merge(T entity) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> T get(Class<T> clazz, Serializable id) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> T execute(HibernateCallback<T> callback) throws HibernateException {
		throw new NotImplementedException("");
	}

	@Override
	public <T> T execute(FlushModeType flushMode, HibernateCallback<T> callback) throws HibernateException {
		throw new NotImplementedException("");
	}

	@Override
	public <T> T executeWithNewSession(HibernateCallback<T> callback) throws HibernateException {
		throw new NotImplementedException("");
	}

	@Override
	public void flush() {

	}

	@Override
	public <T> List<T> find(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushModeType flushMode) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> T findUnique(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushModeType flushMode) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushModeType flushMode) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> List<T> find(String hql, @Nullable FlushModeType flushMode) {
		throw new NotImplementedException("");
	}

	@Override
	public <T> Iterator<T> iterate(String hql, @Nullable FlushModeType flushMode) {
		throw new NotImplementedException("");
	}
}
