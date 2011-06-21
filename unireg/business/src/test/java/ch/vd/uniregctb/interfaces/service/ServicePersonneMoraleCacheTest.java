package ch.vd.uniregctb.interfaces.service;

import net.sf.ehcache.CacheManager;
import org.junit.Test;

import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ServicePersonneMoraleCacheTest extends BusinessTest {

	private DataEventService dataEventService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dataEventService = getBean(DataEventService.class, "dataEventService");
	}

	@Test
	public void testInvalidationCacheSurEvenementPM() throws Exception {

		final long idPM = 12345L;

		// Création du service PM et d'un cache sur ce service
		final MockServicePM servicePM = new MockServicePM() {
			@Override
			protected void init() {
				final MockPersonneMorale pm = new MockPersonneMorale();
				pm.setRaisonSociale("Ma petit entreprise");
				pm.setNumeroEntreprise(idPM);
				addPM(pm);
			}
		};

		final ServicePersonneMoraleCache cache = new ServicePersonneMoraleCache();
		cache.setCacheManager(getBean(CacheManager.class, "ehCacheManager"));
		cache.setCacheName("servicePM");
		cache.setUniregCacheManager(getBean(UniregCacheManager.class, "uniregCacheManager"));
		cache.setTarget(servicePM);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();

		// 1) on demande l'entreprise au cache
		{
			final PersonneMorale pm = cache.getPersonneMorale(idPM);
			assertNotNull(pm);
			assertEquals("Ma petit entreprise", pm.getRaisonSociale());
		}

		// 2) on change la raison sociale de l'entreprise
		{
			final MockPersonneMorale pm = new MockPersonneMorale();
			pm.setRaisonSociale("Ma moyenne entreprise");
			pm.setNumeroEntreprise(idPM);
			servicePM.replacePM(pm);
		}

		// 3) on demande une seconde fois l'entreprise au cache => pas de changement
		{
			final PersonneMorale pm = cache.getPersonneMorale(idPM);
			assertNotNull(pm);
			assertEquals("Ma petit entreprise", pm.getRaisonSociale());
		}

		// 4) on notifie à l'event service que la pm a changé
		dataEventService.onPersonneMoraleChange(idPM);

		// 5) on demande une troisième fois l'entreprise au cache => le changement doit être visible
		{
			final PersonneMorale pm = cache.getPersonneMorale(idPM);
			assertNotNull(pm);
			assertEquals("Ma moyenne entreprise", pm.getRaisonSociale());
		}
	}
}
