package ch.vd.unireg.interfaces.organisation.cache;


import java.util.Map;

import net.sf.ehcache.CacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.cache.UniregCacheManagerImpl;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.CivilDataEventService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.organisation.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.stats.MockStatsService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;

public class ServiceEntrepriseCacheTest extends WithoutSpringTest {

	private CallCounterServiceEntreprise target;
	private ServiceEntrepriseCache cache;

	private final CivilDataEventService dataEventService = new CivilDataEventService() {
		@Override
		public void register(CivilDataEventListener listener) {
		}

		@Override
		public void unregister(CivilDataEventListener listener) {
		}

		@Override
		public void onIndividuChange(long id) {
		}

		@Override
		public void onEntrepriseChange(long id) {
		}
	};

	private class CallCounterServiceEntreprise implements ServiceEntrepriseRaw {

		private int historyCounter = 0;
		private final ServiceEntrepriseRaw target;

		public CallCounterServiceEntreprise(ServiceEntrepriseRaw target) {
			this.target = target;
		}

		@Override
		public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws ServiceEntrepriseException {
			historyCounter++;
			return target.getEntrepriseHistory(noEntreprise);
		}

		@Override
		public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws ServiceEntrepriseException {
			return target.getNoEntrepriseFromNoEtablissement(noEtablissementCivil);
		}

		@Override
		public Identifiers getEntrepriseByNoIde(String noide) throws ServiceEntrepriseException {
			return target.getEntrepriseByNoIde(noide);
		}

		@Override
		public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws ServiceEntrepriseException {
			return target.getEntrepriseEvent(noEvenement);
		}

		@Override
		public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceEntrepriseException {
			return target.validerAnnonceIDE(modele);
		}

		@NotNull
		@Override
		public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {
			return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
		}

		@Override
		public void ping() throws ServiceEntrepriseException {
			target.ping();
		}
	}

	@Before
	public void setup() throws Exception {

		target = new CallCounterServiceEntreprise(new MockServiceEntreprise() {
			@Override
			protected void init() {
				 addEntreprise(
						 MockEntrepriseFactory.createEntreprise(101202100L, 872394879L, "Les gentils joueurs de belotte", RegDate.get(2000, 5, 14), null, FormeLegale.N_0109_ASSOCIATION,
						                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2000, 5, 16), StatusRegistreIDE.DEFINITIF,
						                                        TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999995"));
				 addEntreprise(
						 MockEntrepriseFactory.createEntreprise(101202101L, 872394812L, "Le Tarot, c'est rigolo", RegDate.get(2005, 1, 11), null, FormeLegale.N_0109_ASSOCIATION,
						                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2000, 5, 16), StatusRegistreIDE.DEFINITIF,
						                                        TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996"));
			}
		});

		final CacheManager manager = CacheManager.create(ResourceUtils.getFile("classpath:ut/ehcache.xml").getPath());
		manager.clearAll(); // Manager is a singleton, and may exist already

		cache = new ServiceEntrepriseCache();
		cache.setCacheManager(manager);
		cache.setCacheName("serviceEntreprise");
		cache.setUniregCacheManager(new UniregCacheManagerImpl());
		cache.setStatsService(new MockStatsService());
		cache.setTarget(target);
		cache.setDataEventService(dataEventService);
		cache.afterPropertiesSet();
	}

	@Test
	public void testCacheReturnsCorrectOrganisation() {
		long id = 101202100L;
		final EntrepriseCivile entrepriseCivileFromService = target.getEntrepriseHistory(id);
		final EntrepriseCivile entrepriseCivileFromCache = cache.getEntrepriseHistory(id);
		assertEquals(entrepriseCivileFromService.getNumeroEntreprise(), entrepriseCivileFromCache.getNumeroEntreprise());
	}

	@Test
	public void testCallTwiceHitServiceOnce() {
		Assert.assertEquals(0, target.historyCounter);

		cache.getEntrepriseHistory(101202100L);
		Assert.assertEquals(1, target.historyCounter);
		cache.getEntrepriseHistory(101202100L);
		Assert.assertEquals(1, target.historyCounter);

		cache.getEntrepriseHistory(101202101L);
		Assert.assertEquals(2, target.historyCounter);
		cache.getEntrepriseHistory(101202101L);
		Assert.assertEquals(2, target.historyCounter);

		cache.getEntrepriseHistory(101202100L);
		Assert.assertEquals(2, target.historyCounter);
	}

	@Test
	public void testCallTwiceOrganisationChangesInBetween() {
		long id = 101202100L;
		Assert.assertEquals(0, target.historyCounter);
		cache.getEntrepriseHistory(id);
		Assert.assertEquals(1, target.historyCounter);
		cache.onEntrepriseChange(id);
		cache.getEntrepriseHistory(id);
		Assert.assertEquals(2, target.historyCounter);
	}

}
