package ch.vd.uniregctb.common;

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

}
