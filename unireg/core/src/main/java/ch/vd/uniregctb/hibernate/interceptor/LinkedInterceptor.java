package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public interface LinkedInterceptor {

	void afterTransactionBegin(Transaction tx);
	void afterTransactionCompletion(Transaction tx);
	void beforeTransactionCompletion(Transaction tx);

	
	boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException;
	boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException;
	boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException;

	void postFlush(Iterator<?> entities) throws CallbackException;
	void preFlush(Iterator<?> entities) throws CallbackException;

}
