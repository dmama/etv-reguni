package ch.vd.unireg.security;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Classe utilitaire pour IAM.
 *
 * Adapté de ati-security pour Spring-Security, évolution de ACEGI
 */
public class IAMUtil {

	protected static final Log logger = LogFactory.getLog(IAMUtil.class);

	/**
	 * Retourne les rôles de l'utilisateur connecté pour l'application 'application' à partir de la liste de tous ses rôles 'allRoles' qui sont séparées par le char "\\x7C" (pipe).
	 */
	static public String[] createRoles(String application, String allRoles) {
		if (application == null) {
			logger.error("createRoles(): application is null");
			return new String[0];
		}

		if (allRoles == null) {
			logger.error("createRoles(): no role found");
			return new String[0];
		}

		final LinkedList<String> result = new LinkedList<String>();

		if (logger.isDebugEnabled()) {
			logger.debug("createRoles(): application=" + application + "; all roles=" + allRoles);
		}


		// Example of roles obtained from IAM:
		// cn=finances-demo-comptable,dc=etat-de-vaud,dc=ch
		// cn=finances-demo-secretaire_d_office,dc=etat-de-vaud,dc=ch
		// cn=finances-demo-juriste,dc=etat-de-vaud,dc=ch
		// cn=finances-demo-prepose,dc=etat-de-vaud,dc=ch

		// (msi 25.10.2012) With the new openSSO IAM, roles are using a simpler format:
		// finances-demo-comptable|finances-demo-secretaire_d_office|finances-demo-juriste

		// Split roles with separator "pipe":
		final String[] roles = allRoles.split("\\x7C");

		for (final String roleLine : roles) {

			final String roleToken;
			if (roleLine.contains("=")) {
				// Old format (with cn= stuff)
				// Don't take "cn=" nor ",dc=.." by splitting:
				String[] roleItems = roleLine.split("[=,]");
				roleToken = roleItems[1];
			}
			else {
				// New IAM format (without cn= stuff)
				roleToken = roleLine;
			}

			// Take only those roles that are relevant to this application:
			if (roleToken.startsWith(application + "-")) {
				// Extract business role by removing application name:
				final String role = roleToken.substring(1 + application.length());
				if (logger.isDebugEnabled()) {
					logger.debug("createRoles(): role=" + role);
				}
				result.add(role);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("createRoles(): Ignoring role=" + roleToken);
				}
			}
		}

		return result.toArray(new String[result.size()]);
	}
}
