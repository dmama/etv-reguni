package ch.vd.uniregctb.utils;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

/**
 * Classe utilitaire offrant certaines facilités de création de références distantes sur des beans (EJB).
 *
 * @author Fabrice Willemin (xcifwi) - SQLI (last modified by $Author: xcifwi $ @ $Date: 2007/09/07 07:51:23 $)
 * @version $Revision: 1.4 $
 */
public class EjbUtils {
	/**
	 * Cr�� et retourne une r�f�rence sur un bean distant.
	 *
	 * @param jndiName le nom JNDI du service EJB.
	 * @return une réérence sur le bean distant.
	 * @throws Exception si un problème survient durant la création de la référence distante sur le bean.
	 */
	public static Object createBean(String jndiName) throws Exception {
		Properties props = new Properties();
		/** Propriété commune à tous les environnements. */
		props.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");

		/** Propriétés pour l'accès au serveur local. */
		props.put(Context.PROVIDER_URL, "t3://localhost:7001");
		//props.put(Context.PROVIDER_URL, "t3://solve05p.etat-de-vaud.ch:65256");
		//props.put(Context.PROVIDER_URL, "t3://solve61v.etat-de-vaud.ch:63226");
		//props.put(Context.PROVIDER_URL, "t3://solve61v:65032");
		//props.put(Context.PROVIDER_URL, "t3://solve61v.etat-de-vaud.ch:63222");
		//props.put(Context.SECURITY_PRINCIPAL, "web-interfaces-user");
		//props.put(Context.SECURITY_PRINCIPAL, "SEDI");
		//props.put(Context.SECURITY_CREDENTIALS, "web-interfaces-user_1014");
			

		// props.put(Context.SECURITY_PRINCIPAL, "tao-interfaces-user");
		// props.put(Context.SECURITY_CREDENTIALS, "tao-interfaces-pwd");

		/** Propriétés pour l'accès au serveur de développement. */
		// props.put(Context.PROVIDER_URL, "t3://solve61v:65012");
		// props.put(Context.SECURITY_PRINCIPAL, "ifotaouser");
		// props.put(Context.SECURITY_CREDENTIALS, "ifotaopwd");
		/** Propriétés pour l'accès au serveur de validation. */
		// props.put(Context.PROVIDER_URL, "t3://solve61v:63202");
		// props.put(Context.SECURITY_PRINCIPAL, "ifotaouser");
		// props.put(Context.SECURITY_CREDENTIALS, "ifotaouser_1014");
		/** Propriétés pour l'accès au serveur d'intégration SIPF. */
		// props.put(Context.PROVIDER_URL, "t3://solve61v:64904");
		// props.put(Context.SECURITY_PRINCIPAL, "sipf-interfaces-user");
		// props.put(Context.SECURITY_CREDENTIALS, "sipf-interfaces-user");
		/** Propriétés pour l'accès au serveur d'intégration Interfaces. */
		// props.put(Context.PROVIDER_URL, "t3://solve61v:65016");
		// props.put(Context.SECURITY_PRINCIPAL, "sipf-interfaces-user");
		// props.put(Context.SECURITY_CREDENTIALS, "sipf-interfaces-user");
		

		
		/** Propriétés pour l'accès au serveur d'intégration Interfaces. */
//		 props.put(Context.PROVIDER_URL, "t3://solve61v:65016");
//		 props.put(Context.SECURITY_PRINCIPAL, "sipf-interfaces-user");
//		 props.put(Context.SECURITY_CREDENTIALS, "sipf-interfaces-user");
		/** Propriétés pour l'accès au serveur de formation Interfaces. */
//		  props.put(Context.PROVIDER_URL, "t3://solve61v:63252");
//		  props.put(Context.SECURITY_PRINCIPAL, "sipf-interfaces-user");
//		  props.put(Context.SECURITY_CREDENTIALS, "sipf-interfaces-user_1014");
		/** Propriétés pour l'accès au serveur de validation Interfaces. */
//		 props.put(Context.PROVIDER_URL, "t3://solve61v:63222");
//		 props.put(Context.SECURITY_PRINCIPAL, "sipf-interfaces-user");
//		 props.put(Context.SECURITY_CREDENTIALS, "sipf-interfaces-user_1014");
		/** Propriétés pour l'accès au serveur de pré-production Interfaces. */
//		 props.put(Context.PROVIDER_URL, "t3://solve61v:63226");
//		 props.put(Context.SECURITY_PRINCIPAL, "sipf-interfaces-user");
//		 props.put(Context.SECURITY_CREDENTIALS, "sipf-interfaces-user_1014");

		
		/** Propriétés pour l'accès au serveur de PROD Interfaces. */
//		 props.put(Context.PROVIDER_URL, "t3://solve05p:65256");
//		 props.put(Context.SECURITY_PRINCIPAL, "web-interfaces-user_3195");
//		 props.put(Context.SECURITY_CREDENTIALS, "web-interfaces-user_8899");
		return createBean(props, jndiName);
	}
	
	
	
	public static Object createBean(Properties props, String jndiName) throws Exception {
		Context context = new InitialContext(props);

		Object object = context.lookup(jndiName);
		Object ejbHome = PortableRemoteObject.narrow(object, EJBHome.class);

		Method method = ejbHome.getClass().getDeclaredMethod("create", null);
		return method.invoke(ejbHome, null);
	}
}