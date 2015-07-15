package ch.vd.unireg.interfaces.organisation.cache;


import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.mock.DefaultMockServiceOrganisation;
import ch.vd.uniregctb.cache.UniregCacheManagerImpl;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.stats.MockStatsService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;

public class ServiceOrganisationCacheTest extends WithoutSpringTest {

	private static Logger LOGGER = LoggerFactory.getLogger(ServiceOrganisationCacheTest.class);

	private CacheMockServiceOrganisation target;
	private ServiceOrganisationCache cache;

	private final DataEventService dataEventService = new DataEventService() {
		@Override
		public void register(DataEventListener listener) {

		}

		@Override
		public void onTiersChange(long id) {

		}

		@Override
		public void onIndividuChange(long id) {

		}

		@Override
		public void onOrganisationChange(long id) {

		}

		@Override
		public void onPersonneMoraleChange(long id) {

		}

		@Override
		public void onDroitAccessChange(long ppId) {

		}

		@Override
		public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {

		}

		@Override
		public void onLoadDatabase() {

		}

		@Override
		public void onTruncateDatabase() {

		}
	};

	public static class CacheMockServiceOrganisation extends DefaultMockServiceOrganisation {
		private int historyCounter = 0;
		@Override
		public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
			historyCounter++;
			return super.getOrganisationHistory(noOrganisation);
		}

		/**
		 * @return the number of time the History service was called.
		 */
		public int getHistoryCounter() {
			return historyCounter;
		}
	}

	@Before
	public void setup() throws Exception {
		target = new CacheMockServiceOrganisation();

		cache = new ServiceOrganisationCache();
		final CacheManager manager = CacheManager.create(ResourceUtils.getFile("classpath:ut/ehcache.xml").getPath());
		manager.clearAll(); // Manager is a singleton, and may exist already
		cache.setCacheManager(manager);
		cache.setCacheName("serviceOrganisation");
		cache.setUniregCacheManager(new UniregCacheManagerImpl());
		cache.setStatsService(new MockStatsService());
		cache.setTarget(target);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
	}

	@Test
	public void testCallTwiceHitServiceOnce() {
		long id = 101202100L;
		Organisation organisation = cache.getOrganisationHistory(id);
		LOGGER.info(String.format("Target called once, return organisation %d.", organisation.getNo()));
		organisation = cache.getOrganisationHistory(id);
		LOGGER.info(String.format("Target called a second time, return organisation %d.", organisation.getNo()));
		assertEquals(1, target.getHistoryCounter());
	}

	@Test
	public void testCallTwiceOrganisationChangesInBetween() {
		long id = 101202100L;
		Organisation organisation = cache.getOrganisationHistory(id);
		LOGGER.info(String.format("Target called once, return organisation %d.", organisation.getNo()));
		cache.onOrganisationChange(id);
		organisation = cache.getOrganisationHistory(id);
		LOGGER.info(String.format("Target called a second time, return organisation %d.", organisation.getNo()));
		assertEquals(2, target.getHistoryCounter());
	}

}
