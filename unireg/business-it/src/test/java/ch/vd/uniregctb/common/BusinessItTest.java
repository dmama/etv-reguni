package ch.vd.uniregctb.common;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {
		ClientConstants.UNIREG_BUSINESS_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CACHE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_DATABASE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CLIENT_WEBSERVICE
})
public abstract class BusinessItTest extends AbstractBusinessTest {

	//private final static Logger LOGGER = Logger.getLogger(AbstractBusinessItTest.class);

	/*
	@BeforeClass
	public static void initEnv() {
		System.getProperties().setProperty(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
		System.getProperties().setProperty(Context.PROVIDER_URL, "t3://localhost:7001");
	}
	*/

	/**
	 * Timeout par défaut pour les attentes de messages JMS dans les tests BIT
	 */
	public static final long JMS_TIMEOUT = 120000;

	private static final Pattern valiPattern = Pattern.compile("( *---.{4}-)");

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
