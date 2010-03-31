package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.HibernateEntity;

public class ModificationLogInterceptor implements ModificationSubInterceptor, InitializingBean {

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

	public void afterPropertiesSet() throws Exception {
		parent.register(this);
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

	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException {

		boolean modified = false;

		final String user = AuthenticationHelper.getCurrentPrincipal();

		if (previousState == null) {
			modified = assignValue("logCreationUser", propertyNames, currentState, user);
			modified = assignValue("logCreationDate", propertyNames, currentState, new Date()) || modified;
		}
		modified = assignValue("logModifUser", propertyNames, currentState, user) || modified;

		return modified;
	}

	public void postFlush() throws CallbackException {
		// rien à faire ici
	}
}
