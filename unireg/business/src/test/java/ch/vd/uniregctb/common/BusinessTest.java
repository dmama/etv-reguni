package ch.vd.uniregctb.common;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServicePM;

@ContextConfiguration(locations = {
	TestingConstants.UNIREG_BUSINESS_UT_JOBS
})
public abstract class BusinessTest extends AbstractBusinessTest {

	// private final static Logger LOGGER = Logger.getLogger(BusinessTest.class);

	protected ProxyServiceCivil serviceCivil;
	protected ProxyServicePM servicePM;
	protected ProxyServiceInfrastructureService serviceInfra;

	protected GlobalTiersIndexer globalTiersIndexer;
	protected GlobalTiersSearcher globalTiersSearcher;

	@Override
	protected void runOnSetUp() throws Exception {

		try {
			serviceCivil = getBean(ProxyServiceCivil.class, "serviceCivilService");
			servicePM = getBean(ProxyServicePM.class, "servicePersonneMoraleService");
			serviceInfra = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
			globalTiersIndexer = getBean(GlobalTiersIndexer.class, "globalTiersIndexer");
			globalTiersSearcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");
			serviceInfra.setUpDefault();

			super.runOnSetUp();
		}
		catch (Exception e) {
			serviceCivil.tearDown();
			servicePM.tearDown();
			serviceInfra.tearDown();
			throw e;
		}
		catch (Throwable t) {
			serviceCivil.tearDown();
			servicePM.tearDown();
			serviceInfra.tearDown();
			throw new Exception(t);
		}
	}

	@Override
	public void onTearDown() throws Exception {

		try {
			super.onTearDown();
		}
		finally {
			/*
			 * Il faut l'enlever apres le onTearDown parce que le endTransaction en a besoin pour faire l'indexation lors du commit()
			 */
			serviceCivil.tearDown();
			servicePM.tearDown();
		}
	}

	@Override
	protected void loadDatabase(String filename) throws Exception {
		try {
			super.loadDatabase(filename);
		}
		catch (Exception e) {
			serviceCivil.tearDown();
			servicePM.tearDown();
			throw e;
		}
	}

	@Override
	protected void removeIndexData() throws Exception {
		globalTiersIndexer.overwriteIndex();
	}

	/**
	 * @throws Exception
	 */
	@Override
	protected void indexData() throws Exception {
		// globalTiersIndexer.indexAllDatabase();
		// Si on Index en ASYNC (on créée des Threads) tout va bien
		// Sinon, avec indexAllDb(), il y a des problemes de OptimisticLock...
		globalTiersIndexer.indexAllDatabaseAsync(null, 1, Mode.FULL, false);
	}
}
