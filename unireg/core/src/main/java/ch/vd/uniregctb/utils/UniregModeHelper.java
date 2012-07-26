package ch.vd.uniregctb.utils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;

/**
 * Bean qui expose les différences modes de fonctionnement d'Unireg (configurés de manière externe ou à la compilation).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class UniregModeHelper implements InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(UniregModeHelper.class);

	private static boolean efactureEnabled = false;
	private static boolean testMode = false;
	private static boolean standalone = false;
	private static String environnement;
	private static String serviceCivilSource;

	/**
	 * @return true si la efacture est activée
	 */
	public static boolean isEfactureEnabled() {
		return efactureEnabled;
	}

	/**
	 * @return <i>vrai</i> si le mode testing est activé; <i>faux</i> autrement.
	 */
	public static boolean isTestMode() {
		return testMode;
	}

	/**
	 * @return <i>vrai</i> si Unireg est compilé en mode standalone (= host-interface et l'esb mockés); <i>faux</i> autrement.
	 */
	public static boolean isStandalone() {
		return standalone;
	}

	public static String getEnvironnement() {
		return environnement;
	}

	public String getServiceCivilSource() {
		return serviceCivilSource;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setServiceCivilSource(String source) {
		UniregModeHelper.serviceCivilSource = source;
	}

	public void setTestMode(String v) {
		testMode = ("true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v));
	}

	public void setStandalone(boolean v) {
		standalone = v;
	}

	public void setEnvironnement(String environnement) {
		UniregModeHelper.environnement = environnement;
	}

	public void setEfactureEnabled(boolean enabled) {
		UniregModeHelper.efactureEnabled = enabled;
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
