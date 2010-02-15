package ch.vd.uniregctb.fiscal.logging;

import java.security.Principal;

import org.apache.log4j.Logger;

/**
 * Classe permettant de tracer les acc�s et les temps d'acc�s aux proxy's
 * coolgen. Cette classe impl�mente le pattern du singleton.
 * 
 * @author Fabrice Willemin (xcifwi) - SQLI (last modified by $Author: xcifwi $ @ $Date:
 *         2007/07/26 13:07:14 $)
 * @version $Revision: 1.5 $
 */
public class PerformancesLogger {
	/** Logger du service de s�curit�. */
	public static PerformancesLogger CACHE_PERFS_LOGGER = new PerformancesLogger("cache.perfs");

	/** Logger du service registre civil. */
	public static PerformancesLogger REGISTRE_CIVIL_SERVICE_PERFS_LOGGER = new PerformancesLogger("registre.civil.service.perfs");

	/** Logger du service registre fiscal. */
	public static PerformancesLogger REGISTRE_FISCAL_SERVICE_PERFS_LOGGER = new PerformancesLogger("registre.fiscal.service.perfs");

	/** Logger du service de s�curit�. */
	public static PerformancesLogger SECURITE_SERVICE_PERFS_LOGGER = new PerformancesLogger("securite.service.perfs");

	/** Logger du service sdi. */
	public static PerformancesLogger SDI_SERVICE_PERFS_LOGGER = new PerformancesLogger("sdi.service.perfs");

	/** Logger du service registre lhr. */
	public static PerformancesLogger REGISTRE_LHR_SERVICE_PERFS_LOGGER = new PerformancesLogger("registre.lhr.service.perfs");

	/** Le s�parateur utilis� pour les messages. */
	private static final String SEPARATEUR = ";";

	/** Le s�parateur utilis� pour les param�tres. */
	private static final String SEPARATEUR_PARAMETRE = "-";

	/** Le logger */
	private Logger logger = null;

	/**
	 * Constructeur.
	 * 
	 * @param nomLogger
	 *            le nom d'un logger.
	 */
	private PerformancesLogger(String nomLogger) {
		logger = Logger.getLogger(nomLogger);
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}
	
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}
	
	/**
	 * Permet de tracer les acc�s et les temps d'acc�s aux proxy's coolgen.
	 * 
	 * @param startTime
	 *            la date d'appel au service.
	 * @param message
	 *            le message � tracer.
	 */
	public void info(long startTime, String message) {
		if (logger.isInfoEnabled()) {
			logger.info(message + SEPARATEUR + (System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * Permet de tracer les acc�s et les temps d'acc�s aux proxy's coolgen.
	 * 
	 * @param startTime
	 *            la date d'appel au service.
	 * @param methodName
	 *            le nom de la m�thode � tracer.
	 * @param parameters
	 *            les param�tres de la m�thode � tracer.
	 */
	public void info(long startTime, String methodName, EnumTypeLogging type, String[] parameters) {
		info(startTime, null, methodName, type, parameters);
	}

	/**
	 * Permet de tracer les acc�s et les temps d'acc�s aux proxy's coolgen.
	 * 
	 * @param startTime
	 *            la date d'appel au service.
	 * @param methodName
	 *            le nom de la m�thode � tracer.
	 * @param parameters
	 *            les param�tres de la m�thode � tracer.
	 */
	public void info(long startTime, Principal user, String methodName, EnumTypeLogging type, String[] parameters) {

		if (logger.isInfoEnabled()) {
			final String paramsStr = paramsToString(parameters);
			String userName = "";
			if (user != null)
				userName = user.getName();
			this.info(startTime, methodName + " - " + type.getName() + SEPARATEUR + userName + "|" + paramsStr);
		}
	}

	public static String paramsToString(String[] parameters) {
		StringBuilder params = new StringBuilder();
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				Object param = parameters[i];
				params.append(param);
				if (i < parameters.length - 1) {
					params.append(SEPARATEUR_PARAMETRE);
				}
			}
		}
		return params.toString();
	}
}
