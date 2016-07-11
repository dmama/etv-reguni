package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public interface LinkedInterceptor {

	public void afterTransactionBegin(Transaction tx);
	public void afterTransactionCompletion(Transaction tx);
	public void beforeTransactionCompletion(Transaction tx);

	
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException;
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException;
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException;

	public void postFlush(Iterator<?> entities) throws CallbackException;
	public void preFlush(Iterator<?> entities) throws CallbackException;

}
