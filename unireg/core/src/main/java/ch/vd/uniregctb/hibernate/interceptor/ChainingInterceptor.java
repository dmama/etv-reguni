package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class ChainingInterceptor implements Interceptor {

	private final List<LinkedInterceptor> chain = new ArrayList<LinkedInterceptor>();

	public void register(LinkedInterceptor i) {
		chain.add(i);
	}

	public void afterTransactionBegin(Transaction tx) {
		for (LinkedInterceptor i : chain) {
			i.afterTransactionBegin(tx);
		}
	}

	public void afterTransactionCompletion(Transaction tx) {
		for (LinkedInterceptor i : chain) {
			i.afterTransactionCompletion(tx);
		}
	}

	public void beforeTransactionCompletion(Transaction tx) {
		for (LinkedInterceptor i : chain) {
			i.beforeTransactionCompletion(tx);
		}
	}

	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		for (LinkedInterceptor i : chain) {
			// Si un renvoie true, on renvoie true
			ret = i.onFlushDirty(entity, id, currentState, previousState, propertyNames, types) || ret;
		}
		return ret;
	}

	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		for (LinkedInterceptor i : chain) {
			// Si un renvoie true, on renvoie true
			ret = i.onLoad(entity, id, state, propertyNames, types) || ret;
		}
		return ret;
	}
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		for (LinkedInterceptor i : chain) {
			// Si un renvoie true, on renvoie true
			ret = i.onSave(entity, id, state, propertyNames, types) || ret;
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public void postFlush(Iterator entities) throws CallbackException {
		for (LinkedInterceptor i : chain) {
			i.postFlush(entities);
		}
	}

	@SuppressWarnings("unchecked")
	public void preFlush(Iterator entities) throws CallbackException {
		for (LinkedInterceptor i : chain) {
			i.preFlush(entities);
		}
	}



	// Unused

	public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		return null;
	}

	public Object getEntity(String entityName, Serializable id) throws CallbackException {
		return null;
	}

	public String getEntityName(Object object) throws CallbackException {
		return null;
	}

	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
		return null;
	}

	public Boolean isTransient(Object entity) {
		return null;
	}

	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
	}

	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
	}

	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
	}

	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
	}

	public String onPrepareStatement(String sql) {
		return sql;
	}

}
