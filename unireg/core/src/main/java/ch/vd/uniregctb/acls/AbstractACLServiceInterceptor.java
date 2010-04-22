package ch.vd.uniregctb.acls;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Intercepteur utilisé pour gérer l'ajout et la suppression d'ACL par AOP.
 *
 * @author Ludovic Bertin
 */
public abstract class AbstractACLServiceInterceptor implements MethodInterceptor, InitializingBean {

	/**
	 * Le nom de la classe à intercepter.
	 */
	private String className = null;

	/**
	 * Le nom de la méthode à intercepter.
	 */
	private String methodName = null;

	/**
	 * Le gestionnaire d'ACL.
	 */
	protected AclManager aclManager = null;

	/**
	 * Positionne le gestionnaire d'ACL.
	 * @param aclManager le gestionnaire d'ACL
	 */
	public void setAclManager(AclManager aclManager) {
		this.aclManager = aclManager;
	}


	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(aclManager, "Le gestionnaire d'ACL doit être injecté.");
		Assert.notNull(className, "Le nom de la classe interceptée doit être injecté.");
		Assert.notNull(methodName, "Le nom de la méthode interceptée doit être injecté.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// Si les noms de la classe et de la méthode correspondent, appel a doInvoke
		if ( ( invocation.getMethod().getDeclaringClass().getName().equals( className ) )
			&& ( invocation.getMethod().getName().equals( methodName ) )
			) {
			return doInvoke(invocation);
		}

		// Sinon invoque normalement la méthode
		else
			return invocation.proceed();
	}

	/**
	 * Appelé par invoke si le nom de la classe et de la méthode correspondent.
	 * @param invocation objet MethodInvocation.
	 */
	@Transactional(rollbackFor = Throwable.class)
	protected abstract Object doInvoke(MethodInvocation invocation) throws Throwable;


	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}


	/**
	 * @param methodName the methodName to set
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
}
