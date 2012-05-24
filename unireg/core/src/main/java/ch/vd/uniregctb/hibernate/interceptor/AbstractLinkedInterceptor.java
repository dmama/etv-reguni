package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;

public class AbstractLinkedInterceptor implements LinkedInterceptor, InitializingBean {

	private ChainingInterceptor chainingInterceptor;
	
	public void setChainingInterceptor(ChainingInterceptor chainedInterceptor) {
		this.chainingInterceptor = chainedInterceptor;
	}
	@Override
	public void afterPropertiesSet() throws Exception {
		chainingInterceptor.register(this);
	}

	
	
	@Override
	public void afterTransactionBegin(Transaction tx) {
	}
	@Override
	public void afterTransactionCompletion(Transaction tx) {
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
		return false;
	}
	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		return false;
	}
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
		return false;
	}

	@Override
	public void postFlush(Iterator<?> entities) throws CallbackException {
	}
	@Override
	public void preFlush(Iterator<?> entities) throws CallbackException {
	}

}
