package ch.vd.unireg.hibernate;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.jetbrains.annotations.Nullable;

public interface HibernateTemplate {

	/**
	 * Remove an entity (to be used with caution !!)
	 * @param entity entity to remove
	 * @param <T> type of the entity
	 */
	<T> void delete(T entity);

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
	 * Calls the callback in the context of the current hibernate session
	 *
	 * @param flushMode flushmode to apply to the execution (will be restored to previous value upon callback termination)
	 * @param callback callback to call
	 * @return value returned by the callback
	 */
	<T> T execute(FlushMode flushMode, HibernateCallback<T> callback) throws HibernateException;

	/**
	 * Calls the callback in the context of a new session created for the occasion
	 * @param callback callback to call
	 * @param <T> return type
	 * @return value returned by the callback
	 */
	<T> T executeWithNewSession(HibernateCallback<T> callback) throws HibernateException;

	/**
	 * Flush the current session
	 */
	void flush();

	/**
	 * Returns the list of entities corresponding to the given HQL request (using named parameters)
	 * @param hql HQL request
	 * @param namedParams named parameters in the HQL request
	 * @param flushMode if set, flushMode to use during the HQL treatment
	 * @param <T> entities' type
	 * @return list of found entities
	 */
	<T> List<T> find(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushMode);

	/**
	 * Returns the unique entity corresponding to the given HQL request (using named parameters)
	 *
	 * @param hql         HQL request
	 * @param namedParams named parameters in the HQL request
	 * @param flushMode   if set, flushMode to use during the HQL treatment
	 * @param <T>         entities' type
	 * @return list of found entities
	 */
	<T> T findUnique(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushMode);

	/**
	 * Returns an iterator on the list of entities corresponding to the given HQL request (using named parameters)
	 * @param hql HQL request
	 * @param namedParams named parameters in the HQL request
	 * @param flushMode if set, flushMode to use during the HQL treatment
	 * @param <T> entities' type
	 * @return list of found entities
	 */
	<T> Iterator<T> iterate(String hql, @Nullable Map<String, ?> namedParams, @Nullable FlushMode flushMode);

	/**
	 * Returns the list of entities corresponding to the given HQL request (no parameters)
	 * @param hql HQL request
	 * @param flushMode if set, flushMode to use during the HQL treatment
	 * @param <T> entities' type
	 * @return list of found entities
	 */
	<T> List<T> find(String hql, @Nullable FlushMode flushMode);

	/**
	 * Returns an iterator on the list of entities corresponding to the given HQL request (no parameters)
	 * @param hql HQL request
	 * @param flushMode if set, flushMode to use during the HQL treatment
	 * @param <T> entities' type
	 * @return list of found entities
	 */
	<T> Iterator<T> iterate(String hql, @Nullable FlushMode flushMode);

}
