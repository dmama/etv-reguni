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

import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;

public class ChainingInterceptor implements Interceptor, Switchable {

	private final ThreadSwitch mySwitch = new ThreadSwitch(true);

	private final List<LinkedInterceptor> chain = new ArrayList<LinkedInterceptor>();

	@Override
	public void setEnabled(boolean enabled) {
		mySwitch.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return mySwitch.isEnabled();
	}

	public List<LinkedInterceptor> getChain() {
		return chain;
	}

	public void register(LinkedInterceptor i) {
		chain.add(i);
	}

	@Override
	public void afterTransactionBegin(Transaction tx) {
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				i.afterTransactionBegin(tx);
			}
		}
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				i.afterTransactionCompletion(tx);
			}
		}
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				i.beforeTransactionCompletion(tx);
			}
		}
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				// Si un renvoie true, on renvoie true
				ret = i.onFlushDirty(entity, id, currentState, previousState, propertyNames, types) || ret;
			}
		}
		return ret;
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				// Si un renvoie true, on renvoie true
				ret = i.onLoad(entity, id, state, propertyNames, types) || ret;
			}
		}
		return ret;
	}
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		boolean ret = false;
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				// Si un renvoie true, on renvoie true
				ret = i.onSave(entity, id, state, propertyNames, types) || ret;
			}
		}
		return ret;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void postFlush(Iterator entities) throws CallbackException {
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				i.postFlush(entities);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void preFlush(Iterator entities) throws CallbackException {
		if (isEnabled()) {
			for (LinkedInterceptor i : chain) {
				i.preFlush(entities);
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append("@").append(Integer.toHexString(hashCode()));
		sb.append("{chain=").append(chain);
		sb.append('}');
		return sb.toString();
	}

// Unused

	@Override
	public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		return null;
	}

	@Override
	public Object getEntity(String entityName, Serializable id) throws CallbackException {
		return null;
	}

	@Override
	public String getEntityName(Object object) throws CallbackException {
		return null;
	}

	@Override
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
		return null;
	}

	@Override
	public Boolean isTransient(Object entity) {
		return null;
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
	}

	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
	}

	@Override
	public String onPrepareStatement(String sql) {
		return sql;
	}

}
