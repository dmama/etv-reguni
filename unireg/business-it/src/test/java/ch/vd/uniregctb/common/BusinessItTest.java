package ch.vd.uniregctb.common;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.jms.EsbMessageValidatorImpl;
import ch.vd.uniregctb.utils.UniregProperties;
import ch.vd.uniregctb.utils.UniregPropertiesImpl;

@ContextConfiguration(locations = {
		BusinessItTestingConstants.UNIREG_BUSINESSIT_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_RAW_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_EXT_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CACHE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_DATABASE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CLIENT_WEBSERVICE
})
public abstract class BusinessItTest extends AbstractBusinessTest {

	//private final static Logger LOGGER = LoggerFactory.getLogger(AbstractBusinessItTest.class);

	/**
	 * DUMMY instance histoire de s'assurer une initialisation correcte des mocks du services infrastructure
	 */
	private static final DefaultMockServiceInfrastructureService DUMMY = new DefaultMockServiceInfrastructureService();

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
			final UniregPropertiesImpl impl = new UniregPropertiesImpl();
			impl.setFilename("../base/unireg-ut.properties");
			impl.afterPropertiesSet();
			uniregProperties = impl;
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

	public static EsbMessageValidator buildEsbMessageValidator(Resource[] sources) throws Exception {
		final EsbMessageValidatorImpl validator = new EsbMessageValidatorImpl();
		validator.setResourceResolver(new ClasspathCatalogResolver());
		validator.setSources(sources);
		return validator;
	}
}
