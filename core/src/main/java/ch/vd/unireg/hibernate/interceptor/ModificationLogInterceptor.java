package ch.vd.unireg.hibernate.interceptor;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

public class ModificationLogInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean {

	private ModificationInterceptor parent;
	private boolean completeOnly = false;

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public boolean isCompleteOnly() {
		return completeOnly;
	}

	public void setCompleteOnly(boolean completeOnly) {
		this.completeOnly = completeOnly;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}

	private boolean assignValue(String methodName, String[] propertyNames, Object[] currentState, Object value) {
		boolean result = false;

		for (int i = 0; i < propertyNames.length; i++) {
			if (methodName.equals(propertyNames[i])) {
				if (!completeOnly || currentState[i] == null) {
					currentState[i] = value;
					result = true; // Modified
				}
			}
		}
		return result;
	}

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
	                        Type[] types, boolean isAnnulation) throws CallbackException {

		boolean modified = false;

		final String user = StringUtils.abbreviate(AuthenticationHelper.getCurrentPrincipal(), LengthConstants.HIBERNATE_LOGUSER);

		if (previousState == null) {
			modified = assignValue("logCreationUser", propertyNames, currentState, user);
			modified = assignValue("logCreationDate", propertyNames, currentState, DateHelper.getCurrentDate()) || modified;
		}
		modified = assignValue("logModifUser", propertyNames, currentState, user) || modified;

		return modified;
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	@Override
	public void suspendTransaction() {
		// rien à faire ici
	}

	@Override
	public void resumeTransaction() {
		// rien à faire ici
	}

	@Override
	public void preTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionRollback() {
		// rien à faire ici
	}
}
