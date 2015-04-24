package ch.vd.uniregctb.migration.pm.utils;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.shared.hibernate.interceptor.AbstractLinkedInterceptor;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.HibernateEntity;

public class ModificationLogInterceptor extends AbstractLinkedInterceptor {

	private static final String LOG_CUSER = "logCreationUser";
	private static final String LOG_CDATE = "logCreationDate";
	private static final String LOG_MUSER = "logModifUser";
	private static final String LOG_MDATE = "logModifDate";

	/**
	 * Si <code>true</code>, n'assigne les valeurs que si elle sont nulles lors du onSave (à la création de l'entité). Lors de modification d'entité, les valeurs sont de toute manière assignées
	 * indépendemment de la valeur de ce paramètre.
	 */
	private boolean completeOnly = false;

	private static boolean assignValue(String methodName, String[] propertyNames, Object[] currentState, Object value, boolean aCompleteOnly) {
		boolean result = false;

		for (int i = 0; i < propertyNames.length; i++) {
			if (methodName.equals(propertyNames[i])) {
				if (!aCompleteOnly || currentState[i] == null) {
					currentState[i] = value;
				}
				result = true; // Modified
				break;
			}
		}
		return result;
	}

	private static boolean objectHasChanged(String[] propertyNames, Object[] currentState, Object[] previousState) {
		boolean changed = false;
		if (previousState == null) { // msi: on a eu ce cas lors de l'envoi des DIs...
			changed = true;
		}
		else {
			for (int i = 0; i < currentState.length; i++) {

				// String name = propertyNames[i];
				Object c = currentState[i];
				Object p = previousState[i];

				if ((c != null && p == null) || (c == null && p != null)) {
					changed = true;
					break;
				}
				else if (c != null && p != null && !c.equals(p)) {
					changed = true;
					break;
				}
			}
		}

		return changed;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
		boolean modified = false;
		if (entity instanceof HibernateEntity && objectHasChanged(propertyNames, currentState, previousState)) {
			modified = assignValue(LOG_MUSER, propertyNames, currentState, AuthenticationHelper.getCurrentPrincipal(), false);
			modified = assignValue(LOG_MDATE, propertyNames, currentState, DateHelper.getCurrentDate(), false) || modified;
		}
		return modified;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] currentState, String[] propertyNames, Type[] types) throws CallbackException {
		boolean modified = false;
		if (entity instanceof HibernateEntity) {
			final String user = AuthenticationHelper.getCurrentPrincipal();
			final Date now = DateHelper.getCurrentDate();
			modified = assignValue(LOG_CUSER, propertyNames, currentState, user, completeOnly);
			modified = assignValue(LOG_MUSER, propertyNames, currentState, user, completeOnly) || modified;
			modified = assignValue(LOG_CDATE, propertyNames, currentState, now, completeOnly) || modified;
			modified = assignValue(LOG_MDATE, propertyNames, currentState, now, completeOnly) || modified;
		}
		return modified;
	}

	/**
	 * Si <code>true</code>, n'assigne les valeurs que si elle sont nulles lors du onSave (à la création de l'entité). Lors de modification d'entité, les valeurs sont de toute manière assignées
	 * indépendemment de la valeur de ce paramètre.
	 */
	public void setCompleteOnly(boolean completeOnly) {
		this.completeOnly = completeOnly;
	}
}