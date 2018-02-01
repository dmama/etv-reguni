package ch.vd.uniregctb.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;

/**
 * Bean qui expose les différences modes de fonctionnement d'Unireg (configurés de manière externe ou à la compilation).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class UniregModeHelper implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(UniregModeHelper.class);

	private static boolean reqdesEnabled = false;
	private static boolean efactureEnabled = false;
	private static boolean testMode = false;
	private static String environnement;

	/**
	 * @return true si la efacture est activée
	 */
	public static boolean isEfactureEnabled() {
		return efactureEnabled;
	}

	public static boolean isReqdesEnabled() {
		return reqdesEnabled;
	}

	/**
	 * @return <i>vrai</i> si le mode testing est activé; <i>faux</i> autrement.
	 */
	public static boolean isTestMode() {
		return testMode;
	}

	public static String getEnvironnement() {
		return environnement;
	}

	public void setTestMode(String v) {
		testMode = ("true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v));
	}

	public void setEnvironnement(String environnement) {
		UniregModeHelper.environnement = environnement;
	}

	public void setEfactureEnabled(boolean enabled) {
		UniregModeHelper.efactureEnabled = enabled;
	}

	public void setReqdesEnabled(boolean reqdesEnabled) {
		UniregModeHelper.reqdesEnabled = reqdesEnabled;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (DateConstants.TIME_OFFSET != 0) {
			if (testMode) {
				LOGGER.warn("+---------------------------------------------------------------------------------------+");
				LOGGER.warn("| Attention ! La date courante de l'application est décalée de " + DateHelper.getTimeOffsetAsString(DateConstants.TIME_OFFSET, false) + " |");
				LOGGER.warn("+---------------------------------------------------------------------------------------+");
			}
			else {
				throw new IllegalArgumentException(
						"Le décalage de la date courante de l'application (variable d'environnement DateConstants.TIME_OFFSET) est interdite en dehors des environnements de testing !");
			}
		}
	}
}
