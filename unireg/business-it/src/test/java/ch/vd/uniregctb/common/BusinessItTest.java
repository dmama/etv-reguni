package ch.vd.uniregctb.common;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;

@ContextConfiguration(locations = {
		ClientConstants.UNIREG_BUSINESS_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CACHE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_DATABASE,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_INTERFACES,
		BusinessItTestingConstants.UNIREG_BUSINESSIT_CLIENT_WEBSERVICE
})
public abstract class BusinessItTest extends AbstractBusinessTest {

	//private final static Logger LOGGER = Logger.getLogger(AbstractBusinessItTest.class);

	protected GlobalTiersIndexer globalTiersIndexer;

	@Override
	public void onSetUp() throws Exception {
		globalTiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
		super.onSetUp();
	}

	/*
	@BeforeClass
	public static void initEnv() {
		System.getProperties().setProperty(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
		System.getProperties().setProperty(Context.PROVIDER_URL, "t3://localhost:7001");
	}
	*/

	@Override
	protected void removeIndexData() throws Exception {
		globalTiersIndexer.overwriteIndex();
	}

	/**
	 * @throws Exception
	 */
	@Override
	protected void indexData() throws Exception {
		globalTiersIndexer.indexAllDatabaseAsync(null, 1, Mode.FULL, false);
	}

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
