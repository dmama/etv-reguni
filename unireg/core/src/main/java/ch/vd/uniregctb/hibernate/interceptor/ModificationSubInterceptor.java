package ch.vd.uniregctb.hibernate.interceptor;

import java.io.Serializable;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Interface définissant la méthode appelée par la classe ModificationNotifier lorsqu'une entité hibernate est ajoutée/modifiée dans la base
 * de données d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface ModificationSubInterceptor {

	/**
	 * Cette méthode est appelée lorsque une entité hibernate est modifié/sauvé.
	 */
	boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException;


	/**
	 * Cette méthode est appelée après que Hibernate ait flushé les objets modifié dans la base de données.
	 */
	void postFlush() throws CallbackException;
}
