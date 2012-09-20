package ch.vd.uniregctb.common;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.utils.UniregProperties;

@ContextConfiguration(locations = {
		BusinessItTestingConstants.UNIREG_BUSINESSIT_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_RAW_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_EXT_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CACHE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_DATABASE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CLIENT_WEBSERVICE
})
public abstract class BusinessItTest extends AbstractBusinessTest {

	//private final static Logger LOGGER = Logger.getLogger(AbstractBusinessItTest.class);

	/**
	 * Timeout par défaut pour les attentes de messages JMS dans les tests BIT
	 */
	public static final long JMS_TIMEOUT = 120000;

	private static final Pattern valiPattern = Pattern.compile("( *---.{4}-)");

	protected UniregProperties uniregProperties;

	protected BusinessItTest() {
		initProps();
	}

	private void initProps() {
		try {
			uniregProperties = new UniregProperties();
			uniregProperties.setFilename("file:../base/unireg-ut.properties");
			uniregProperties.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Supprime l'éventuel pattern "---VALI-" ou "---TEST-" ajouté aux DB de validation/test.
	 */
	public static String trimValiPattern(String string) {
		if (string == null) {
			return null;
		}
		else {
			return valiPattern.matcher(string).replaceAll("").trim();
		}
	}

	/**
	 * Supprime l'éventuel pattern "---VALI-" ou "---TEST-" ajouté aux DB de validation/test.
	 */
	public static String trimValiPatternToNull(String string) {
		if (string == null) {
			return null;
		}
		else {
			return StringUtils.trimToNull(valiPattern.matcher(string).replaceAll(""));
		}
	}

}
