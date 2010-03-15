package ch.vd.uniregctb.tiers;

import org.aopalliance.intercept.MethodInvocation;

import ch.vd.uniregctb.acls.AbstractACLServiceInterceptor;
import ch.vd.uniregctb.acls.TiersPermission;

/**
 * Intercepteur de la création d'un tiers. 
 * Intercepte la méthode save de la classe TiersServiceImpl.
 * 
 * @author Ludovic Bertin
 */
public class TiersCreationInterceptor extends AbstractACLServiceInterceptor {

	/**
	 * Constructeur.
	 */
	public TiersCreationInterceptor() {
		super();
		setClassName( TiersService.class.getName() );
		setMethodName("save");
	}
	
	@Override
	protected Object doInvoke(MethodInvocation invocation) throws Throwable{
		
		// Invocation de la méthode
		Object result = invocation.proceed();

		// Récupération du Tiers
		Tiers tiers = (Tiers) result;
		
		// Ajout de l'ACL : interdiction de lire le tiers correspondant a soi meme
		aclManager.addPermission(tiers, "xvdtst033", TiersPermission.READ, false);
		
		return result;
	}

}
