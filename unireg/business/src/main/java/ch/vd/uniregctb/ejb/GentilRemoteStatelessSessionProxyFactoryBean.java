package ch.vd.uniregctb.ejb;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean;
import org.springframework.remoting.RemoteLookupFailureException;

/**
 * [UNIREG-2496] Un SimpleRemoteStatelessSessionProxyFactoryBean qui attend gentiment que le serveur EJB soit up au lien de cracher une RemoteLookupFailureException.
 */
public class GentilRemoteStatelessSessionProxyFactoryBean extends SimpleRemoteStatelessSessionProxyFactoryBean {

	private static final Logger LOGGER = Logger.getLogger(GentilRemoteStatelessSessionProxyFactoryBean.class);

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		while(true) {
			try {
				return super.invoke(invocation);
			}
			catch (RemoteLookupFailureException e) {
				// si le serveur est introuvable, on part du principe qu'il va être démarré rapidemment et on attend indéfiniment qu'il nous réponde.
				LOGGER.warn("Le serveur " + getJndiTemplate().getEnvironment().get("java.naming.provider.url") + " est introuvable. On attend 1 minute...");
				Thread.sleep(20000); // 20 secondes d'attente + 40 secondes de timeout sur le connexion = 1 essai toutes les minutes
			}
		}
	}
}
