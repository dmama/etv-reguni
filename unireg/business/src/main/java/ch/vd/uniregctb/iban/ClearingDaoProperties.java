package ch.vd.uniregctb.iban;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * Implémentation de la DAO pour tester la validité d'un code d'un établissement bancaire.
 *
 * @author Ludovic Bertin (OOSphere)
 */
public class ClearingDaoProperties implements ClearingDao, InitializingBean {

	//private static final Logger LOGGER = Logger.getLogger(ClearingDaoProperties.class);

	/**
	 * Le nom du fichier contenant les numéros de clearing.
	 */
	private static final String CLEARING_NUMBERS_FILE = "iban/clearingNumbers.properties";

	/**
	 * Les numéros de clearing connus
	 */
	private Properties clearingNumbers = null;

	/**
	 * Initialise la liste des numéros connus en lisant le fichier properties.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		InputStream clearingNumbersFile = ClearingDaoProperties.class.getClassLoader().getResourceAsStream(CLEARING_NUMBERS_FILE);
		clearingNumbers = new Properties();
		clearingNumbers.load( clearingNumbersFile );
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.common.iban.ClearingDao#isNumeroClearingValid(java.lang.String)
	 */
	@Override
	public boolean isNumeroClearingValid(String numeroClearing) {
		if (numeroClearing == null) {
			return false;
		}

		// on retire les zeros inutiles
		while(!StringUtils.isEmpty(numeroClearing) && numeroClearing.charAt(0) == '0') {
			numeroClearing = numeroClearing.substring(1);
		}
		return clearingNumbers.containsKey(numeroClearing);
	}

}
