package ch.vd.uniregctb.hibernate;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.jetbrains.annotations.Nullable;

public interface HibernateTemplate {

	/**
	 * Save or update given entity, returning value after save
	 * @param entity entity to save or update
	 * @param <T> entity type
	 * @return saved entity
	 */
	<T> T merge(T entity);

	/**
	 * Retrieve a entity from the DB based on its class and identifier
	 * @param clazz entity's class
	 * @param id entity's identifier
	 * @param <T> entity type
	 * @return retrieved entity
	 */
	<T> T get(Class<T> clazz, Serializable id);

	/**
	 * Calls the callback in the context of the current hibernate session
	 * @param callback callback to call
	 * @param <T> return type
	 * @return value returned by the callback
	 */
	<T> T execute(HibernateCallback<T> callback) throws HibernateException;

	/**
	 * Flush the current session
	 */
	void flush();

	/**
	 * Returns the list of entities corresponding to the given HQL request
	 * @param hql HQL request
	 * @param params parameters in the HQL request
	 * @param flushMode if set, flushMode to use during the HQL treatment
	 * @param <T> entities' type
	 * @return list of found entities
	 */
	<T> List<T> find(String hql, @Nullable Object[] params, @Nullable FlushMode flushMode);

	/**
	 * Returns an iterator on the list of entities corresponding to the given HQL request
	 * @param hql HQL request
	 * @param params parameters in the HQL request
	 * @param flushMode if set, flushMode to use during the HQL treatment
	 * @param <T> entities' type
	 * @return list of found entities
	 */
	<T> Iterator    <T> iterate(String hql, @Nullable Object[] params, @Nullable FlushMode flushMode);
}
